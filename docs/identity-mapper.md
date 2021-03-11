Identity mapper is the engine that will turn the data acquired from your users into JSONs to represent their identity.

You can define the schema of your identity map in the ``./identity.json`` file (or change it with the ``IDENTITY_FILE`` env variable).

It's defined with a JSON object like this:

```json
{
  "nickname": "discord.username || google.firstName || vis.user_name",
  "email": "discord.email || google.email || vis.email_address",
  "avatar": "discord.avatar || google.photoURL || vis.photo_link_png"
}
```

Note that each key's value is a Javascript expression. A variable with the data for all your [Authentication Providers](providers.md) will be defined, with the name of the Provider's id and the value of an object with the user data.

In the example above, we set that the nickname of the identity will be ``discord.username``, and then use the ``||`` operator to fallback to ``google.firstName`` if the left value is falsy. Then, do it again to ``vis.user_name``.

Those are crude JS expressions, so be careful with what you write there, because those expressions will be evaluated in reAuth and can expose your system to issues if you do something dangerous.

The expression must return ``null``, ``undefined`` or ``string``. Other types will cause system failures.

Null and undefined values will just be skipped of the user's final JSON identity.
