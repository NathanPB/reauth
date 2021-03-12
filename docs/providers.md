An Authentication Provider, in the scope of oAuth2, is a server that implements the oAuth 2.0 protocol and can serve as a third party authentication system, for social logins, for instance. Examples of Authentication Providers are services Discord, Google, Twitch.

# Defining the Authentication Providers

## providers.json

You can specify the Authentication Providers that reauth will be using by editing the ``providers.json`` file, that should be located in the working directory of the reAuth JVM process, but can be changed by using the ``PROVIDERS_FILE`` environment variable.

## Authentication Provider JSON Model

The ``providers.json`` file should be a JSON array of JSON objects with the following model:

```json
{
  "id": "example",
  "clientId": "foobar",
  "clientSecret": "foo123124172389bar",
  "scopes": "identify email",
  "authorizeURL": "https://example.com/oauth2/authorize",
  "tokenURL": "https://example.com/oauth2/token",
  "userDataURL": "https://example.com/api/user/me",
  "linkageField": "email",
  "idField": "id",
  "dataAccessRules": {
    "username": ["identity"]
  }
}
```

### Authentication Provider Model

| Key             | Type               | Description |
| --------------- |------------------- |------------ |
| id              | string             | The namespace which this Auth Provider will be using |
| clientId        | string             | The client ID of the Auth Provider, as defined in [RFC 6478 A.1](https://tools.ietf.org/html/rfc6749#appendix-A.1) |
| clientSecret    | string             | The client secret of the Auth Provider, as defined in [RFC 6478 A.2](https://tools.ietf.org/html/rfc6749#appendix-A.2) |
| scope           | string             | The scopes that the resource owner will be asked for when logging in with this Auth Provider, as defined in [RFC 6478 A.4](https://tools.ietf.org/html/rfc6749#appendix-A.4) |
| authorizeURL    | string (URI valid) | The Authorize Endpoint of this Auth Provider, as defined in [RFC 6478 4.1.1](https://tools.ietf.org/html/rfc6749#section-4.1.1) |
| tokenURL        | string (URI valid) | The Token Endpoint of this Auth Provider, as defined in [RFC 6478 4.1.3](https://tools.ietf.org/html/rfc6749#section-4.1.3) |
| userDataURL     | string (URI valid) | The endpoint which reAuth will expect to retrieve the resource owner information from |
| linkageField    | string             | The data linkage field, described in [Linking Accounts](account-linking.md#linking-accounts). |
| idField         | string             | The user id field, described in [User ID Generation](account-linking.md#user-id-generation). |
| dataAccessRules | Access Rule Object | The access rules to map the data that each scope should access (TODO not properly documented yet)
