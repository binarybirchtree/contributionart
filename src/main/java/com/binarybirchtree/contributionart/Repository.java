package com.binarybirchtree.contributionart;

import java.io.IOException;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.Logger;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;

public class Repository implements AutoCloseable {
  private static final Logger LOGGER = Logger.getLogger(Repository.class.getName());

  public class GitException extends Exception {
    GitException (String message) {
      super(message);
    }
  }

  private Git git;

  @Override
  public void close () {
    git.close();
  }

  ///
  /// @param[in] directory Directory of the repository.
  /// @param[in] branch Branch to use.
  ///
  public Repository (Path directory, String branch) throws IOException, GitException {
    try {
      Git.init().setDirectory(directory.toFile()).setBare(false).call();
      git = Git.open(directory.toFile());
      LOGGER.info(String.format("Initialized Git repository at '%s'.", directory));

      if (branch != null) {
        git.checkout().setName(branch).call();
        LOGGER.info(String.format("Checked out branch '%s'.", branch));
      }
    }
    catch (GitAPIException error) {
      throw new GitException(error.toString());
    }
  }

  ///
  /// @param[in] directory Directory of the repository.
  ///
  public Repository (Path directory) throws IOException, GitException {
    this(directory, null);
  }

  ///
  /// Adds commits to the repository so as to form an illustrated version of the specified matrix
  /// when viewed as a GitHub contribution graph.
  ///
  /// @param[in] matrix Matrix to illustrate.
  /// @param[in] factor Scaling factor.
  /// @param[in] name User name.
  /// @param[in] email Email address.
  /// @param[in] timestamp Timestamp containing the current date to use to render the matrix.
  ///
  public void illustrate (Matrix matrix, int factor, String name, String email, ZonedDateTime timestamp) throws GitException {
    ZonedDateTime now = ZonedDateTime.now();

    // Start from the earliest date, which corresponds to the first value in the definition matrix.
    ZonedDateTime current = timestamp
    .truncatedTo(ChronoUnit.DAYS)
    .with(WeekFields.SUNDAY_START.dayOfWeek(), DayOfWeek.values().length)
    .minusDays(Matrix.AREA - 1);

    for (Matrix.Value value : matrix) {
      // Skip values that correspond to dates later than the specified timestamp.
      if (current.isBefore(now)) {
        // The number of commits to generate for a particular date depends on
        // the corresponding value in the definition matrix and the scaling factor.
        int weight = value.weight() * factor;

        try {
          PersonIdent identity = new PersonIdent(name, email, Date.from(current.toInstant()), TimeZone.getTimeZone(current.getZone()));

          for (int i = 0; i < weight; ++i) {
            git.commit().setMessage("").setAuthor(identity).setCommitter(identity).call();
          }
        }
        catch (GitAPIException error) {
          throw new GitException(error.toString());
        }

        LOGGER.info(String.format("Created %d commit%s with timestamp %s.", weight, weight > 1 ? "s" : "", current));
      }
      current = current.plusDays(1);
    }
  }

  public void illustrate (Matrix matrix, int factor, String name, String email) throws GitException {
    illustrate(matrix, factor, name, email, ZonedDateTime.now(ZoneOffset.UTC));
  }
}
