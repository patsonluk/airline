Test deploy

An opensource airline game. 

Live at https://www.airline-club.com/


![Screenshot 1](https://user-images.githubusercontent.com/2895902/74759887-5a966380-522e-11ea-9e54-2252af63d5ea.gif)
![Screenshot 2](https://user-images.githubusercontent.com/2895902/74759902-6124db00-522e-11ea-9f81-8b4af7f7027e.gif)
![Screenshot 3](https://user-images.githubusercontent.com/2895902/74759935-739f1480-522e-11ea-9323-e84095177d5a.gif)

## Setup

1. Install Java openjdk 11
1. Install MySQL 8 and then create database `airline_v2_1`, create a user `sa`, for password you might use `admin` or change it to something else. Make sure you change the corresponding password logic in the code to match that (https://github.com/patsonluk/airline/blob/master/airline-data/src/main/scala/com/patson/data/Constants.scala#L184)
1. Navigate to `airline-data` and run `activator publishLocal`. If you see [encoding error](https://github.com/patsonluk/airline/issues/267), add character-set-server=utf8mb4 to your /etc/my.cnf and restart mysql. it's a unicode characters issue, see https://stackoverflow.com/questions/10957238/incorrect-string-value-when-trying-to-insert-utf-8-into-mysql-via-jdbc
1. In `airline-data`, run `activator run`, 
    1. Then, choose the one that runs `MainInit`. It will take awhile to init everything.
1. Set `google.mapKey` in [`application.conf`](https://github.com/patsonluk/airline/blob/master/airline-web/conf/application.conf#L69) with ur google map API key value. Be careful with setting budget and limit, google gives some free credit but it CAN go over and you might get charged!
1. For the "Flight search" function to work, install elastic search 7.x, see https://www.elastic.co/guide/en/elasticsearch/reference/current/install-elasticsearch.html . For windows, I recommand downloading the zip archive and just unzip it - the MSI installer did not work on my PC
1. For airport image search and email service for user pw reset - refer to https://github.com/patsonluk/airline/blob/master/airline-web/README
1. Now run the background simulation by staying in `airline-data`, run `activator run`, select option `MainSimulation`. It should now run the backgroun simulation
1. Open another terminal, navigate to `airline-web`, run the web server by `activator run`
1. The application should be accessible at `localhost:9000`

## Nginx Proxy w/ Cloudflare HTTPS

In Cloudflare go to your domain and then SSL/TLS > Origin Server. Click Create Certificate > Generate private key and CSR with Cloudflare > Drop down choose ECC > Create

Save your Origin Certificate and your Private Key to a file. Example:

Orgin Certificate: domain.com.crt

Private Key: domain.com.key

Example nginx virtualhost conf file:

```
server {

  listen 443 ssl http2;
  listen [::] ssl http2;
  server_name domain.com;

  ssl_certificate      /usr/local/nginx/conf/ssl/domain.com/domain.com.crt;
  ssl_certificate_key  /usr/local/nginx/conf/ssl/domain.com/domain.com.key;

  add_header X-Frame-Options SAMEORIGIN;
  add_header X-Xss-Protection "1; mode=block" always;
  add_header X-Content-Type-Options "nosniff" always;
  add_header Referrer-Policy "strict-origin-when-cross-origin";

  access_log /home/nginx/domains/domain.com/log/access.log combined buffer=256k flush=5m;
  error_log /home/nginx/domains/domain.com/log/error.log;

  location /assets  {
    alias    /home/airline/airline-web/public/;
    access_log on;
    expires 30d;
  }

  location / {
    proxy_pass http://localhost:9000;
    proxy_pass_header Content-Type;
    proxy_read_timeout     60;
    proxy_connect_timeout  60;
    proxy_redirect         off;

    # Allow the use of websockets
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection 'upgrade';
    proxy_set_header Host $host;
    proxy_cache_bypass $http_upgrade;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
  }

}
```

## Banners

Self notes, too much trouble for other people to set it up right now. Just do NOT enable the banner. (disabled by default, to enable change `bannerEnabled` in `application.conf`

For the banners to work properly, need to setup google photo API. Download the oauth json and put it under airline-web/conf. Then run the app, the log should show an oauth url, use it, then it should generate a token under airline-web/google-tokens. Now for server deployment, copy the oauth json `google-oauth-credentials.json` to `conf` AND the google-tokens (as folder) to the root of `airline-web`.

## Attribution

Some icons by [Yusuke Kamiyamane](http://p.yusukekamiyamane.com/). Licensed under a [Creative Commons Attribution 3.0 License](http://creativecommons.org/licenses/by/3.0/)
