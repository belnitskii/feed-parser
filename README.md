## Feed Parser Bot

A Telegram bot that aggregates articles from RSS feeds, ranks them by relevance using a set of weighted keywords, and delivers the **top 5 posts** to the user. Relevance is determined through lemmatization and scoring. Articles are cached to ensure fast responses without reprocessing content on every request.

Users can **rate articles** (like / dislike / neutral) and **save them to Instapaper**, a service for reading saved content across devices — including e-readers. Rated and saved articles are excluded from future recommendations.

## Daily Routine

The bot operates on a scheduled daily cycle:

- **07:00** – Fetches fresh content from all connected RSS feeds, clears outdated entries, and updates the article cache.
- **08:00** – Sends the user the **top 5 most relevant articles**, based on the latest data and previous interactions.

## How to Run

1. Configure the following in the `application.properties` file:
   - Instapaper email and password  
   - Telegram bot token and name  
   - Your Telegram user ID

2. Add desired RSS feed URLs to the `FeedSources` class.

3. Build and run the project:

   ```bash
   mvn clean package -DskipTests
   nohup java -jar target/feed-parser-0.0.1-SNAPSHOT.jar > app.log 2>&1 &
