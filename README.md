# About
This a backend side of project called Pepper Minter created by MinterMania and Minter Folio for Minter Twitter challenge 2019
This is our frontend (flutter)
This is our idea and todo list (ru)

#How to run it
In case you wanna run this app on your side, compile and run it by yourself via Maven or just download[ the latest jar file](https://github.com/mintermania/pepperminter-backend/tree/master/target " the latest jar file") (with dependencies) and run it (*java -jar file.jar*)!

## API
This project is both API and implementation of biplive technology created using [Kotlin](https://kotlinlang.org/ "Kotlin") and [SparkJava](http://sparkjava.com/ "SparkJava")
For now there are two endoints for our API:
- [GET] /tweets
 - *first* ([new|old], default *new*) - sort order
 - *profiles* ([true|false] default *true*) - if you want to get profiles with tweets
 - *on_page* ([integer] default *50*) - amount of tweets on one page
 - *page* ([integer] default *1*) - current page
 - *block* ([integer] default null) - comfortable for pagination when you dont wanna mess up with new tweets, so its just the block to which you wanna get tweets
 - *from* ([minter address] default null) - return tweets only from this address
 - *to* ([minter address] default null) - return tweets only to this address
 - *skip_air* ([true|false] default *false*) - if you want to get only tweets with recepient
 
 Some clarification about response:
  - *next* ([true|false]) - if there is a next page
  - *prev* ([true|false]) - if there is a previous page
  - *block* ([integer]) - all tweets are before this block (get it on page 1 and use in next pages)
  
- [GET] /profile
 - *address* ([minter address] required) - get profile for this address
 
## LICENSE
MIT
