package cn.yan.sdk.infrastructure.git;

import cn.yan.sdk.types.utils.RandomStringUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.stream.Stream;


public class GitCommand {

    private final Logger logger = LoggerFactory.getLogger(GitCommand.class);

    private final String githubReviewLogUri;

    private final String githubToken;

    private final String project;

    private final String branch;

    private final String author;

    private final String message;

    public GitCommand(String githubReviewLogUri, String githubToken, String project, String branch, String author, String message) {
        this.githubReviewLogUri = githubReviewLogUri;
        this.githubToken = githubToken;
        this.project = project;
        this.branch = branch;
        this.author = author;
        this.message = message;
    }

    public String diff() throws IOException, InterruptedException {
        // openai.itedus.cn
        ProcessBuilder logProcessBuilder = new ProcessBuilder("git", "log", "-1", "--pretty=format:%H");
        logProcessBuilder.directory(new File("."));
        Process logProcess = logProcessBuilder.start();

        BufferedReader logReader = new BufferedReader(new InputStreamReader(logProcess.getInputStream()));
        String latestCommitHash = logReader.readLine();
        logReader.close();
        int logExitCode = logProcess.waitFor();
        if (logExitCode != 0 || latestCommitHash == null) {
            throw new IllegalStateException("Failed to get the latest commit, exit code: " + logExitCode);
        }

        ProcessBuilder diffProcessBuilder = new ProcessBuilder("git", "diff", latestCommitHash + "^", latestCommitHash);
        diffProcessBuilder.directory(new File("."));
        Process diffProcess = diffProcessBuilder.start();

        StringBuilder diffCode = new StringBuilder();
        BufferedReader diffReader = new BufferedReader(new InputStreamReader(diffProcess.getInputStream()));
        String line;
        while ((line = diffReader.readLine()) != null) {
            diffCode.append(line).append("\n");
        }
        diffReader.close();

        int exitCode = diffProcess.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Failed to get diff, exit code:" + exitCode);
        }

        return diffCode.toString();
    }

    public String commitAndPush(String recommend) throws Exception {
        Path cloneDirectory = Files.createTempDirectory("openai-code-review-");
        UsernamePasswordCredentialsProvider credentialsProvider =
                new UsernamePasswordCredentialsProvider("x-access-token", githubToken);
        String repositoryUri = githubReviewLogUri.endsWith(".git")
                ? githubReviewLogUri
                : githubReviewLogUri + ".git";

        try (Git git = Git.cloneRepository()
                .setURI(repositoryUri)
                .setDirectory(cloneDirectory.toFile())
                .setCredentialsProvider(credentialsProvider)
                .call()) {
            String dateFolderName = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            File dateFolder = cloneDirectory.resolve(dateFolderName).toFile();
            if (!dateFolder.mkdirs() && !dateFolder.isDirectory()) {
                throw new IOException("Failed to create review log directory: " + dateFolder);
            }

            String fileName = sanitize(project) + "-" + sanitize(branch) + "-" + sanitize(author)
                    + "-" + System.currentTimeMillis() + "-" + RandomStringUtils.randomNumeric(4) + ".md";
            File newFile = new File(dateFolder, fileName);
            try (FileWriter writer = new FileWriter(newFile)) {
                writer.write(recommend);
            }

            git.add().addFilepattern(dateFolderName + "/" + fileName).call();
            git.commit().setMessage("Add code review log " + fileName).call();
            git.push().setCredentialsProvider(credentialsProvider).call();

            String logBranch = git.getRepository().getBranch();
            logger.info("openai-code-review git commit and push done! {}", fileName);
            return stripGitSuffix(githubReviewLogUri) + "/blob/" + logBranch + "/"
                    + dateFolderName + "/" + fileName;
        } finally {
            deleteDirectory(cloneDirectory);
        }
    }

    private static String sanitize(String value) {
        String sanitized = value.replaceAll("[^a-zA-Z0-9._-]", "_");
        return sanitized.isEmpty() ? "unknown" : sanitized;
    }

    private static String stripGitSuffix(String uri) {
        return uri.endsWith(".git") ? uri.substring(0, uri.length() - 4) : uri;
    }

    private static void deleteDirectory(Path directory) {
        try (Stream<Path> paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    // Temporary files do not affect a completed review.
                }
            });
        } catch (IOException e) {
            // Temporary files do not affect a completed review.
        }
    }

    public String getProject() {
        return project;
    }

    public String getBranch() {
        return branch;
    }

    public String getAuthor() {
        return author;
    }

    public String getMessage() {
        return message;
    }

}
