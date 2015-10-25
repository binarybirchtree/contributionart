package com.binarybirchtree.contributionart;

import java.io.IOException;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.Iterator;
import java.util.stream.StreamSupport;

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
    Path repo = folder.newFolder().toPath();
    Matrix matrix = new Matrix(file);
    final int factor = 20;
    final String name = "name";
    final String email = "email";

    // Generate commits in a temporary repository.
    try (Repository repository = new Repository(repo)) {
      repository.illustrate(matrix, factor, name, email);
    }

    // Verify that the state of the repository is as expected.
    try (Git git = Git.open(repo.toFile())) {
      // Start from the earliest date for which commits were generated.
      ZonedDateTime current = ZonedDateTime.now(ZoneOffset.UTC)
      .truncatedTo(ChronoUnit.DAYS)
      .with(WeekFields.SUNDAY_START.dayOfWeek(), DayOfWeek.values().length)
      .minusDays(Matrix.AREA);

      // Prepare to iterate through the definition matrix alongside the commit log,
      // as the values in the definition matrix affect how many commits should have been generated.
      Iterator<Matrix.Value> values = matrix.iterator();
      Matrix.Value value;
      int cell_iterations = 0;
      int commit_count = 0;

      // Iterate through the commit log, starting from the earliest commit.
      for (RevCommit commit : Lists.reverse(Lists.newArrayList(git.log().call()))) {
        if (cell_iterations == 0) {
          Assert.assertTrue(values.hasNext());
          value = values.next();
          cell_iterations = value.weight() * factor;
          current = current.plusDays(1);
        }

        Assert.assertEquals(current.toInstant(), Instant.ofEpochSecond(commit.getCommitTime()));

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
          }

          private void validate_name () {
            Assert.assertEquals(name, identity.getName());
          }

          private void validate_email () {
            Assert.assertEquals(email, identity.getEmailAddress());
          }

          ///
          /// @param[in] timestamp Expected timestamp.
          ///
          private void validate_timestamp (ZonedDateTime timestamp) {
            Assert.assertEquals(timestamp.toInstant(), identity.getWhen().toInstant());
          }

          ///
          /// @param[in] timestamp Expected timestamp.
          ///
          public void validate (ZonedDateTime timestamp) {
            validate_name();
            validate_email();
            validate_timestamp(timestamp);
          }
        }

        (new IdentityValidator(commit.getAuthorIdent())).validate(current);
        (new IdentityValidator(commit.getCommitterIdent())).validate(current);

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
