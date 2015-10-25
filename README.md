# ContributionArt

Places custom-designed artwork on the GitHub contribution graph.

## Building

### Install build dependencies

```Shell
sudo apt-get install openjdk-8-jdk gradle
```

### Build, run unit tests, and create wrapper scripts

```Shell
gradle --daemon build installDist
```

## Running

Note:
ContributionArt makes no assumptions about whether Git is installed on the host system or not.
If you do have Git installed and have specified a name and email in the Git `--global` configuration, you can use the values of `git config user.name` and `git config user.email` as in the following example.
If not, no problem - you can explicitly specify a name and email to use.

```Shell
build/install/contributionart/bin/contributionart \
--matrix contribution.art \
--repo repo \
--name "$(git config user.name)" \
--email "$(git config user.email)" \
--factor 20
```

## Demo

```Shell
./contributionart.sh
```

## Author

Binary Birch Tree

