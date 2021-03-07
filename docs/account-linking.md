# Linking Accounts

reAuth will automagically handle the account linking of new incoming users by comparing content of the ``linkageField``, specified in the [Authentication Provider documentation](providers.md).

Let's take, for instance, the following [Auth Providers](providers.md):

``providers.json``

```json
[
  { "id": "google", ..., "linkageField": "email" },
  { "id": "discord", ..., "linkageField": "email" },
  { "id": "vis", ..., "linkageField": "email_address" }
]
```

Let's now say that a user is attempting to log in for the first time using his Google account, which has the email of ``nathan@g.com``.

Since Nathan do not have any Identities yet, reAuth will create for him the first Identity with the provider of ``google`` and the data that could be obtained.

Now Nathan wants to log in using Discord. His Discord account is registered with the same email, so the Discord API will (should) be providing that same e-mail address.

Oh no, now Nathan will have an account duplication!

Not actually, because, since we set the ``linkageField``, reAuth will be able to correctly manage Nathan's multiple identities, and, when asked for, all the identities that has the same ``linkageField`` will be returned. No data loss or data merge between different Auth Providers will occur.

Note that, also, in the provider of id ``vis`` (stands for Very Intuitive Service), the linkageField is different from the two above. reAuth also understands that, and, for instance, will consider that the identities below belongs to the same resource owner:

```json
{
  "_id": "...",
  "provider": "google",
  "data": { "email": "nathan@g.com", ... }
}

{
  "_id": "...",
  "provider": "discord",
  "data": { "email": "nathan@g.com", ... }
}

{
  "_id": "...",
  "provider": "vis",
  "data": { "email_address": "nathan@g.com" }
}
```

All the Identities above belongs to the same Resource Owner, and will be treated as so.

# User ID generation

The Authentication Provider ``idField`` field describes where the User Unique ID will be located at the data that reAuth will be retrieving from the Auth Provider's resources. For instance:

``GET https://vis.wtf/api/me``

```json
{
  "user_identifier": "2983",
  "username": "Nathan",
  "email": "nathan@g.com",
  "age": 20,
  "description": "dumb"
}
```

``providers.json``

```json
[
  ...,
  { "id": "vis", ..., "idField": "user_identifier" }
]
```

In that case, reAuth will attempt to check the database to see if we already have an Identity from "vis" that has the "``user_identifier``" field set to ``2983``. If so, they will be treated as the same profile and incoming data will be merged into this Identity.

This behavior is experimental and may be changed in a near future, since its extremely close to [Linking Accounts](#linking-accounts)
