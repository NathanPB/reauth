## Requirements

- MongoDB (tested with Mongo 4.2)
- JVM
- A web application which you will be using ReAuth
- Some login providers

## Environment Variables

ReAuth will need you to set up some environment variables to work with:

```
MONGO_CONN_STRING=mongodb://user:pwd@database.example.com:27017/auth
APP_CONSENT_URI=http://localhost:300/authorize/consent
```

``MONGO_CONN_STRING`` - The connection string for the Mongo database

``APP_CONSENT_URI`` - The URL that your login screen consent screen will be at. Look at the examples for ~~how to invalidate cache tutorial~~ examples

## Basic Endpoints

The only thing that differs from the oAuth 2.0 protocol ([RFC 6749](https://tools.ietf.org/html/rfc6749)) is that instead of just one ``Authorize`` endpoints, we will have one for each [Authorization Provider](providers.md) you set up.

E.g.

``GET /oauth/authorize/discord``

``GET /oauth/authorize/google``

``GET /oauth/authorize/vis``

You should pass query parameters exactly as a standard oAuth system would expect:

``GET /oauth/authorize/vis?client_id=007a0bae-926f-4c9c-90d6-bbf533281f98&response_type=code&scope=identity&redirect_uri=http://localhost:3000/callback``

After the resource owner completed the process, it will be redirected to your ``redirect_uri`` with the ``code`` to make the code exchange.
