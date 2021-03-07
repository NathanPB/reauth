You can define which scopes reAuth should accept in the ``./scopes.json`` file, which can be changed using the ``SCOPES_FILE`` env variable.

This file should contain a JSON Array of String and just strings.

Each string put in here will be accepted as a valid scope by the reAuth oAuth 2.0 implementation.
