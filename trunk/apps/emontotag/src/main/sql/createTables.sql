create table tag_user (
   id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
   username VARCHAR(30),
   password VARCHAR(30)
   );
   
create table tag_options (
   id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
   name VARCHAR(100),
   description VARCHAR(500)
   );
   
create table tag (
   id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,	
   username VARCHAR(30),
   tag_option_id INT,
   ontology_id VARCHAR(30), --such as EMO_0000042
   tag_timestamp TIMESTAMP(8)
   );