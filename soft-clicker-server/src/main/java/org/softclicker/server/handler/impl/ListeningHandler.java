package org.softclicker.server.handler.impl;

import org.apache.log4j.Logger;
import org.softclicker.message.dao.impl.SoftClickAnswerDAOImpl;
import org.softclicker.message.dao.impl.SoftClickBroadcastDAOImpl;
import org.softclicker.message.dto.SoftClickAnswer;
import org.softclicker.server.entity.Answer;
import org.softclicker.server.entity.Question;
import org.softclicker.server.entity.User;
import org.softclicker.server.exception.SoftClickerException;
import org.softclicker.server.exception.SoftClickerRuntimeException;
import org.softclicker.server.gui.MainApplication;
import org.softclicker.server.gui.controllers.quiz.AnswerListener;
import org.softclicker.server.handler.ServerHandler;
import org.softclicker.transport.handler.MessageHandler;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class ListeningHandler implements ServerHandler {

    private volatile Thread serverThread;
    private static final Logger log = Logger.getLogger(ListeningHandler.class);
    ServerSocket serverSocket;
    private boolean stopped = false;
    public ListeningHandler(int port, Question listeningQuestion, AnswerListener answerListener) throws SoftClickerException {

        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            throw new SoftClickerException("Cannot create TCP socket.");
        }
        this.serverThread = new Thread() {
            @Override
            public void run() {
                while (true && !stopped) {
                    try (
                            Socket connectionSocket = serverSocket.accept();
                            BufferedReader receivedMsg = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream(), StandardCharsets.UTF_8));
                            DataOutputStream sendingMsg = new DataOutputStream(connectionSocket.getOutputStream());
                    ) {
                        //read message
                        String answerText = null;
                        try {
                            answerText = receivedMsg.readLine();
                            log.info("received text" + answerText);
                        } catch (IOException e) {
                            log.error("Cannot read answer text.", e);
                        }
                        //construct response
                        byte[] response = "Invalid Response".getBytes();
                        if (answerText != null) {
                            MessageHandler messageHandler = new MessageHandler(new SoftClickBroadcastDAOImpl(),
                                    new SoftClickAnswerDAOImpl());
                            SoftClickAnswer softClickAnswer = messageHandler.decodeAnswer(answerText.getBytes());
                            response = processMessage(answerListener, listeningQuestion, Answer.ANSWERS.values()[softClickAnswer.getAnswerOption().ordinal()].toString());
                        }
                        //send response
                        try {
                            sendingMsg.write(response, 0, response.length);
                        } catch (IOException e) {
                            log.error("Cannot send response.", e);
                        }
                    } catch (IOException e) {
                        if(!stopped)
                        throw new SoftClickerRuntimeException("Cannot create socket.", e);
                    }
                }
            }
        };
    }

    private byte[] processMessage(AnswerListener answerListener, Question listeningQuestion, String answerText) {
        User loggedUser = MainApplication.getInstance().getLoggedUser();
        Answer answer = new Answer(-1, answerText, listeningQuestion, loggedUser, new Date());
        try {
            //answer received, trigger the callback
            answerListener.answerReceived(answer);
            return "Answer saved".getBytes();
        } catch (SoftClickerException e) {
            return "Answer saving failed".getBytes();
        }
    }

    @Override
    public void start() {
        this.serverThread.start();
    }

    @Override
    public void stop() {
        if(serverSocket != null)
        {
            try {
                stopped = true;
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.serverThread = null;
    }

    @Override
    public boolean isRunning() {
        return this.serverThread.isAlive();
    }
}
