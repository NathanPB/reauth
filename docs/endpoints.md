# Endpoints

## Authorize

**Path:** GET /oauth/authorize/:provider

Starts the authorization flow with the ``:provider``.

E.g. ``GET /oauth/authorize/discord?client_id=123&response_type=code&scope=identify&redirect_uri=http://localhost:3000``

Delegates all the properties from [RFC 6749 Section 4.1.1](https://tools.ietf.org/html/rfc6749#section-4.1.1)

### Headers

- **``Reauth-Origin`` (optional):** Sets the URL to be used as the origin of the request. Must be a valid URI. If not present, a URI will be assembled based in the ``Origin`` header.



## Token

**Path:** POST /oauth/token

Delegates all the properties from [RFC 6749 Section 4.1.3](https://tools.ietf.org/html/rfc6749#section-4.1.3)
