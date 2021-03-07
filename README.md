# The reAuth Project

## Summary

ReAuth is a work in progress project that aims to simplify as much as possible the life of developers that are implementing Social Logins with multiple platforms in their apps.

ReAuth encourages the adoption of the oAuth 2.0 protocol and Social Login, while heavily discouraging the use of username/password logins and credential storage.

Let's build a safer web, together.

## How

ReAuth works by abstracting and summarizing the oAuth 2.0 implementation of various different authentication servers into just a single oAuth 2.0 implementation.

So, instead of having to manage by hand the tokens, endpoints and resources for dozens of different providers, developers can just consume the reAuth endpoints and it will do everything for them.

## Getting Started and Documentations

You can find the ReAuth documentations [here](docs). There you will find some markdown files that explain how some steps work to help getting you started.

You can also find a little example using ReAuth in a NodeJS application [here](example).


## Work in Progress

ReAuth is a Work in Progress project and should be ready for production soon. We are looking for collaborators.

NodeJS, JVM, React and NextJS wrappers for ReAuth coming soon!

## License

ReAuth is licensed under the GNU Lesser General Public License v0.3. Feel free to do whatever you want with the code found here as long as you follow the license.

```
Copyright (c) 2020 - Nathan P. Bombana 

This file is part of ReAuth.

ReAuth is free software: you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

ReAuth is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public License
along with ReAuth.  If not, see <https://www.gnu.org/licenses/>.
```
