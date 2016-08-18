// This file is part of ContributionArt.
// Copyright (C) 2015, 2016 Binary Birch Tree
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.

package com.binarybirchtree.contributionart;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
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

  private Path directory;
  private Git git;
  private String name;
  private String email;

  @Override
  public void close () {
    git.close();
  }

  ///
  /// @param[in] directory Directory of the repository.
  /// @param[in] name User name.
  /// @param[in] email Email address.
  ///
  public Repository (Path directory, String name, String email) throws IOException, GitException {
    this.directory = directory;
    this.name = name;
    this.email = email;

    try {
      Git.init().setDirectory(directory.toFile()).setBare(false).call();
      git = Git.open(directory.toFile());
      LOGGER.info(String.format("Initialized Git repository at '%s'.", directory));
    }
    catch (GitAPIException error) {
      throw new GitException(error.toString());
    }
  }

  ///
  /// Adds commits to the repository so as to form an illustrated version of the specified matrix
  /// when viewed as a GitHub contribution graph.
  ///
  /// @param[in] matrix Matrix to illustrate.
  /// @param[in] factor Scaling factor.
  /// @param[in] timestamp Timestamp containing the current date to use to render the matrix.
  ///
  public void illustrate (Matrix matrix, int factor, ZonedDateTime timestamp) throws GitException, IOException {
    ZonedDateTime now = ZonedDateTime.now();

    // Start from the earliest date, which corresponds to the first value in the definition matrix.
    ZonedDateTime current = timestamp
    .truncatedTo(ChronoUnit.DAYS)
    .with(WeekFields.SUNDAY_START.dayOfWeek(), DayOfWeek.values().length)
    .minusDays(Matrix.AREA);

    for (Matrix.Value value : matrix) {
      current = current.plusDays(1);

      // Skip values that correspond to dates later than the specified timestamp.
      if (current.isBefore(now) || current.isEqual(now)) {
        // The number of commits to generate for a particular date depends on
        // the corresponding value in the definition matrix and the scaling factor.
        int weight = value.weight() * factor;

        try {
          PersonIdent identity = identity(current);
          for (int i = 0; i < weight; ++i) {
            git.commit().setMessage("").setAuthor(identity).setCommitter(identity).call();
          }
        }
        catch (GitAPIException error) {
          throw new GitException(error.toString());
        }

        LOGGER.info(String.format("Created %d commit%s with timestamp %s.", weight, weight > 1 ? "s" : "", current));
      }
    }

    create_file(directory.resolve("README.md"), README, timestamp.truncatedTo(ChronoUnit.DAYS));
  }

  public void illustrate (Matrix matrix, int factor) throws GitException, IOException {
    illustrate(matrix, factor, ZonedDateTime.now(ZoneOffset.UTC));
  }

  ///
  /// Creates a file at the specified path with the specified contents and commit timestamp.
  ///
  /// @param[in] file File path.
  /// @param[in] contents File contents.
  /// @param[in] timestamp Commit timestamp.
  ///
  protected void create_file (Path file, String contents, ZonedDateTime timestamp) throws GitException, IOException {
    try (BufferedWriter writer = Files.newBufferedWriter(file)) {
      writer.write(contents);
    }

    try {
      git.add().addFilepattern(directory.relativize(file).toString()).call();
      PersonIdent identity = identity(timestamp);
      git.commit().setMessage(String.format("Added %s.", file.getFileName().toString())).setAuthor(identity).setCommitter(identity).call();
    }
    catch (GitAPIException error) {
      throw new GitException(error.toString());
    }

    LOGGER.info(String.format("Created file '%s' with timestamp %s and contents '%s'.", file, timestamp, contents));
  }

  ///
  /// @param[in] timestamp Timestamp.
  /// @return PersonIdent for the specified timestamp.
  ///
  private PersonIdent identity (ZonedDateTime timestamp) {
    return new PersonIdent(name, email, Date.from(timestamp.toInstant()), TimeZone.getTimeZone(timestamp.getZone()));
  }

  public static String README =
  "# Contribution Graph Artwork\n\n" +
  "This repository was programmatically generated by [ContributionArt](https://github.com/binarybirchtree/contributionart/), " +
  "a tool which allows custom-designed artwork to be displayed on the GitHub contribution graph.\n";
}
