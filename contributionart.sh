#!/bin/bash -e

echo "Running Gradle."
gradle --daemon build installDist

echo "Running ContributionArt."
build/install/contributionart/bin/contributionart \
--matrix contribution.art \
--repo repo \
--name "$(git config user.name)" \
--email "$(git config user.email)" \
--factor 20

