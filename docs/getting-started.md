## Requirements

- MongoDB (tested with Mongo 4.2)
- JVM
- A web application which you will be using ReAuth
- Some login providers

## Environment Variables

ReAuth will need you to set up some environment variables to work with:

```
BASE_URL=http://localhost:6660
MONGO_CONN_STRING=mongodb://user:pwd@database.example.com:27017/auth
APP_AUTHORIZE_URL=http://localhost:300/authorize
APP_CONSENT_URL=http://localhost:300/authorize/consent
```


``BASE_URL`` - The base URL of the reAuth server. The need of this should be removed soon.

``MONGO_CONN_STRING`` - The connection string for the Mongo database

``APP_AUTHORIZE_URL`` - The URL that your login screen will be at. Look at the examples for ~~free candy~~ examples

``APP_CONSENT_URL`` - The URL that your login screen consent screen will be at. Look at the examples for ~~how to invalidate cache tutorial~~ examples
