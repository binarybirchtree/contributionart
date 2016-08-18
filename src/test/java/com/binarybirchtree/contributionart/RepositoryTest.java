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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.Iterator;
import java.util.List;
import java.util.stream.StreamSupport;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Assert;
import org.junit.Test;

public class RepositoryTest extends BaseTest {
  @Test
  public void validate_commits () throws IOException, Matrix.FileFormatException, Repository.GitException, GitAPIException {
    final int factor = 20;
    final String name = "name";
    final String email = "email";

    ///
    /// Encapsulates commit-validation logic.
    ///
    class CommitValidator {
      final ZonedDateTime timestamp;

      ///
      /// @param[in] commit Commit to validate.
      /// @param[in] timestamp Expected timestamp.
      /// @param[in] message Expected message.
      ///
      CommitValidator (RevCommit commit, ZonedDateTime timestamp, String message) {
        this.timestamp = timestamp;

        Assert.assertEquals(timestamp.toInstant(), Instant.ofEpochSecond(commit.getCommitTime()));
        Assert.assertEquals(message, commit.getFullMessage());

        new IdentityValidator(commit.getAuthorIdent());
        new IdentityValidator(commit.getCommitterIdent());
      }

      ///
      /// Contains shared validation logic used for both Author and Committer identities.
      ///
      class IdentityValidator {
        private PersonIdent identity;

        ///
        /// @param[in] identity Identity to validate.
        ///
        public IdentityValidator (PersonIdent identity) {
          this.identity = identity;

          validate_name();
          validate_email();
          validate_timestamp();
        }

        private void validate_name () {
          Assert.assertEquals(name, identity.getName());
        }

        private void validate_email () {
          Assert.assertEquals(email, identity.getEmailAddress());
        }

        private void validate_timestamp () {
          Assert.assertEquals(timestamp.toInstant(), identity.getWhen().toInstant());
        }
      }
    }

    Path repo = folder.newFolder().toPath();
    Matrix matrix = new Matrix(file);
    ZonedDateTime today = ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.DAYS);

    // Generate commits in a temporary repository.
    try (Repository repository = new Repository(repo, name, email)) {
      repository.illustrate(matrix, factor);
    }

    // Verify that the state of the repository is as expected.
    try (Git git = Git.open(repo.toFile())) {
      // Start from the earliest date for which commits were generated.
      ZonedDateTime current = today
      .with(WeekFields.SUNDAY_START.dayOfWeek(), DayOfWeek.values().length)
      .minusDays(Matrix.AREA);

      // Prepare to iterate through the definition matrix alongside the commit log,
      // as the values in the definition matrix affect how many commits should have been generated.
      Iterator<Matrix.Value> values = matrix.iterator();
      Matrix.Value value;
      int cell_iterations = 0;
      int commit_count = 0;

      // Retrieve the list of commits, sorted from earliest to latest.
      List<RevCommit> commits = Lists.reverse(Lists.newArrayList(git.log().call()));
      Assert.assertFalse(commits.isEmpty());

      // Validate the README commit.
      String readme = "README.md";
      new CommitValidator(Iterables.getLast(commits), today, String.format("Added %s.", readme));
      commits.remove(commits.size() - 1);
      Assert.assertEquals(Repository.README, new String(Files.readAllBytes(repo.resolve(readme))));

      // Iterate through the commit log, starting from the earliest commit.
      for (RevCommit commit : commits) {
        if (cell_iterations == 0) {
          Assert.assertTrue(values.hasNext());
          value = values.next();
          cell_iterations = value.weight() * factor;
          current = current.plusDays(1);
        }

        new CommitValidator(commit, current, "");

        ++commit_count;
        --cell_iterations;
      }

      // Determine the expected commit count and compare it with the actual commit count.
      int expected_commit_count = StreamSupport.stream(matrix.spliterator(), false)
      .map(Matrix.Value::weight)
      .reduce(0, (accumulator, element) -> accumulator + element * factor);

      while (values.hasNext()) {
        expected_commit_count -= values.next().weight() * factor;
      }

      Assert.assertEquals(expected_commit_count, commit_count);
    }
  }
}
