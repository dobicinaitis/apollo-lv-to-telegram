# apollo.lv to telegram

A "small" Java command-line application that reads the [RSS feed](https://www.apollo.lv/rss) of the Latvian news site
[Apollo.lv](https://www.apollo.lv) and posts article headlines to Telegram Channel
"[Apollo.lv | Vairāk nekā ziņas](https://t.me/apollo_lv)".

The application is designed to be executed periodically using `cron` or, in this instance, a GitHub
Actions [workflow](https://github.com/dobicinaitis/apollo-lv-to-telegram/actions/workflows/sync.yml).

## CLI usage

```commandline
java -jar apollo-lv-to-telegram.jar [-dhV] [-u=URL] [-t=TOKEN] [-c=CHANNEL_ID] [-s=FILE] [-r=LABEL] [-e=CATEGORY[,CATEGORY...]]...

OPTIONS
  -u, --url=URL                   News feed RSS URL (default: https://www.apollo.lv/rss).
  -t, --token=TOKEN               Telegram bot token.
  -c, --channel-id=CHANNEL_ID     Telegram Channel ID.
  -s, --status-file=FILE          File to store information about the last processed article.
                                  Used to prevent posting duplicates on repeated runs.
  -r, --read-button-label=LABEL   Label for the "Read" button in Telegram.
  -e, --exclude-categories=CATEGORY[,CATEGORY...]
                                  List of article categories to exclude.
  -V, --version                   Print version information and exit.
  -d, --debug                     Print debug information.
  -h, --help                      Show this help message and exit.
```

## Environment variables

Most CLI parameters can also be provided via environment variables.

**Mapping**

| Option                 | Environment variable             | Value example             |
|------------------------|----------------------------------|---------------------------|
| `--url`                | `FEED_READER_URL`                | https://www.apollo.lv/rss |
| `--token`              | `FEED_READER_TOKEN`              | 1234567890:ABCDEF...      |
| `--channel-id`         | `FEED_READER_CHANNEL_ID`         | -1234567890000            |
| `--status-file`        | `FEED_READER_STATUS_FILE`        | last-sync-status.json     |
| `--read-button-label`  | `FEED_READER_READ_BUTTON_LABEL`  | Read                      |
| `--exclude-categories` | `FEED_READER_EXCLUDE_CATEGORIES` | sports,horoscopes         |
| `--debug`              | `FEED_READER_DEBUG`              | `true`/`false`            |

This can be useful when running the application inside a container, to hide sensitive information from CI/CD logs,
console history, etc.

## Run your own

Want to run your own instance or test some changes locally? Here's how.

### Create a Bot 🤖

Start off by messaging [@BotFather](https://t.me/botfather) on Telegram to create your
own [Bot](https://core.telegram.org/bots).

### Create a Channel 📰

Create a [Telegram Channel](https://telegram.org/tour/channels) and add the bot as an administrator.

### Find the Channel ID 🔍

Discover your Channel ID by posting a message to the channel and
visiting `https://api.telegram.org/bot<token>/getUpdates` (substitute `<token>` with the one that you got
from [BotFather](https://t.me/botfather))

You'll find your Channel ID in the `result › message › chat › id` field.

### Build the application 🔨

> **Note**: You will need Java 17 or later to build the project locally.

Clone this repository and build the application:

```shell
git clone git@github.com:dobicinaitis/apollo-lv-to-telegram.git
cd apollo-lv-to-telegram
./gradlew build
```

Alternately you can download a pre-build version from
the [Releases](https://github.com/dobicinaitis/apollo-lv-to-telegram/releases/latest) page.

### Run the application 🚀

```shell
java -jar build/libs/apollo-lv-to-telegram-<version>.jar \
    --token <your-bot-token> \
    --channel-id <your-channel-id> \
    --status-file last-sync-status.json \
    --debug
```

That's it! 🎉 Headlines from the RSS feed should appear in your Channel. \
Utilize the `--status-file` option with the same file to avoid posting duplicates on subsequent runs.

## joy++

<a href="https://www.buymeacoffee.com/dobicinaitis" target="_blank"><img src="https://cdn.buymeacoffee.com/buttons/default-orange.png" alt="Buy Me A Coffee" height="41" width="174"></a>
