Feed Parser Bot

A Telegram bot that aggregates articles from RSS feeds, evaluates their relevance based on a set of weighted keywords, and displays the top 5 posts to the user. The evaluation is performed through text lemmatization and score calculation. All articles are cached, allowing the bot to respond quickly without re-analyzing content on each request.

Users can rate articles (like / dislike / neutral) and optionally save them to Instapaper — a service for reading saved content across devices. Rated articles are excluded from future recommendations.
