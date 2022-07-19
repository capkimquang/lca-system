package com.lca.analyzerlcaspring.service;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

@Service
public class CloneServiceImpl implements CloneService {
    Logger LOGGER = LoggerFactory.getLogger(CloneServiceImpl.class);

    @Override
    public void cloneRepo(int assignmentId, String assignmentUrl) throws Exception {
        LOGGER.info("Start clonning repository (" + assignmentUrl + ") of assignment " + assignmentId + ".");
        File file = new File("src/main/resources/gitclone/assignment-" + assignmentId + "/");
        if (file.exists()) {
            LOGGER.info("Repository exists. Cleaning before clonning.");
            cleanRepo(assignmentId);
        }
        try {
            Git.cloneRepository()
                    .setURI(assignmentUrl)
                    .setDirectory(file)
                    .call();
            LOGGER.info("Finish clonning repository (" + assignmentUrl + ") of assignment " + assignmentId + ".");
        } catch (GitAPIException gitAPIException) {
            throw new Exception("Cloned repository (" + assignmentUrl + ") of assignment " + assignmentId + " with exception " + gitAPIException);
        }
    }

    @Override
    public void cleanRepo(int assignmentId) throws Exception {
        LOGGER.info("Start cleaning repository of assignment " + assignmentId + ".");
        try {
            String osName = System.getProperty("os.name");
            ProcessBuilder builder = new ProcessBuilder();
            if (osName.toLowerCase().startsWith("windows")) {
                builder.command("cmd.exe", "/c", "rm -r assignment-" + assignmentId);
            } else {
                builder.command("sh", "-c", "rm -r assignment-" + assignmentId);
            }
            builder.directory(new File("src/main/resources/gitclone"));
            Process process = builder.start();
            StreamGobbler streamGobbler = new StreamGobbler(process.getInputStream(), System.out::println);
            Executors.newSingleThreadExecutor().submit(streamGobbler);
            int exitCode = process.waitFor();
            assert exitCode == 0;
            LOGGER.info("Finish cleaning repository of assignment " + assignmentId + ".");
        } catch (Exception exception) {
            throw new Exception("Cleaning repository of assignment " + assignmentId + " with exception: " + exception);
        }
    }

    private static class StreamGobbler implements Runnable {
        private InputStream inputStream;
        private Consumer<String> consumer;

        public StreamGobbler(InputStream inputStream, Consumer<String> consumer) {
            this.inputStream = inputStream;
            this.consumer = consumer;
        }

        @Override
        public void run() {
            new BufferedReader(new InputStreamReader(inputStream)).lines().forEach(consumer);
        }
    }
}
