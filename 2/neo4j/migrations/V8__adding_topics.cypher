

CREATE CONSTRAINT topic_name_unique IF NOT EXISTS
FOR (t:Topic) REQUIRE t.name IS UNIQUE;

// 2. Create the Topic nodes
UNWIND [
  "arts", "arts  culture", "black voices", "business", "college", "comedy",
  "crime", "culture  arts", "divorce", "education", "entertainment",
  "environment", "fifty", "food  drink", "good news", "green",
  "healthy living", "home  living", "impact", "latino voices", "media",
  "money", "parenting", "parents", "politics", "queer voices", "religion",
  "science", "sports", "style", "style  beauty", "taste", "tech",
  "the worldpost", "travel", "us news", "weddings", "weird news",
  "wellness", "women", "world news", "worldpost"
] AS topicName
MERGE (t:Topic {name: topicName});