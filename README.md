An opensource airline game. 

Live at https://www.airline-club.com/

Version 2 alpha at https://v2.airline-club.com


![Screenshot 1](https://user-images.githubusercontent.com/2895902/74759887-5a966380-522e-11ea-9e54-2252af63d5ea.gif)
![Screenshot 2](https://user-images.githubusercontent.com/2895902/74759902-6124db00-522e-11ea-9f81-8b4af7f7027e.gif)
![Screenshot 3](https://user-images.githubusercontent.com/2895902/74759935-739f1480-522e-11ea-9323-e84095177d5a.gif)



## Setup
1. install git :D of course
1. clone this repo ;D , to setup for V2 use `git checkout v2`
1. Install at least java development kit 8+
1. The 2 main projects are : airline-web (all the front-end stuff) and airline-data (the backend simulation).(Optional) If you want to import them to Scala IDE (if you want to code), goto the folder of those and run `activator eclipse` to generate the eclipse project files and then import those projects into your IDE
1. This runs on mysql db (install veresion 5.x, i heard newest version 8.x? might not work). install Mysql server and then create database `airline_v2_1`, create a user `sa`, for password you might use `admin` or change it to something else. Make sure you change the corresponding password logic in the code to match that (https://github.com/patsonluk/airline/blob/master/airline-data/src/main/scala/com/patson/data/Constants.scala#L99)
1. `airline-web` has dependency on `airline-data`, hence navigate to `airline-data` and run `activator publishLocal`. If you see [encoding error](https://github.com/patsonluk/airline/issues/267), add character_set_server=utf8mb4 to your /etc/my.cnf and restart mysql. it's a unicode characters issue, see https://stackoverflow.com/questions/10957238/incorrect-string-value-when-trying-to-insert-utf-8-into-mysql-via-jdbc
1. You would need to initialize the DB and data on first run. In `airline-data`, run `activator run`, then choose the one that runs `MainInit`. It will take awhile to init everything.
1. Set `google.mapKey` in [`application.conf`](https://github.com/patsonluk/airline/blob/master/airline-web/conf/application.conf#L69) with ur google map API key value. This might not necessary if you run on local host (?) , but if you want to host a public one, your probably need to apply one from google and enable the map API. Be careful with setting budget and limit, google gives some free credit but it CAN go over and you might get charged!
1. (Optional) For the "Flight search" function to work, install elastic search 7.x, see https://www.elastic.co/guide/en/elasticsearch/reference/current/install-elasticsearch.html . For windows, I recommand downloading the zip archive and just unzip it - the MSI installer did not work on my PC
1. (Optional) For airport image search and email service for user pw reset - refer to https://github.com/patsonluk/airline/blob/master/airline-web/README
1. Now run the background simulation by staying in `airline-data`, run `activator run`, select option `MainSimulation`. It should now run the backgroun simulation
1. Open another terminal, navigate to `airline-web`, run the web server by `activator run`
1. The application should be accessible at `localhost:9000`

## Banners
Self notes, too much trouble for other people to set it up right now. Just do NOT enable the banner. (disabled by default, to enable change `bannerEnabled` in `application.conf`

For the banners to work properly, need to setup google photo API. Download the oauth json and put it under airline-web/conf. Then run the app, the log should show an oauth url, use it, then it should generate a token under airline-web/google-tokens. Now for server deployment, copy the oauth json `google-oauth-credentials.json` to `conf` AND the google-tokens (as folder) to the root of `airline-web`. 


## Attribution
1. Some icons by [Yusuke Kamiyamane](http://p.yusukekamiyamane.com/). Licensed under a [Creative Commons Attribution 3.0 License](http://creativecommons.org/licenses/by/3.0/)
1. Flag icons by [famfamfam](https://github.com/legacy-icons/famfamfam-flags)
