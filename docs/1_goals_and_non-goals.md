# Goals and non-goals

## Motivation

The idea of this project was born when I kept on hearing people asking for
some non-trivial Scala project, which would have a complexity greater than
some TODO-lists or Pet Shops. While this kind of projects are good at showing
tha principles they fail to demonstrate how certain solutions scale, and what
needs to be done when some difficulties arrive - it's hard to even meet certain
difficulties in a small project dealing with only the simples cases, but once
project gets big enough, they appear and require the developer's attention.

I decided on Reddit-clone when I saw some people complaining about Reddit and
considering leaving it. While I do not fool myself that I could write some
serious alternative (just think of the costs of infrastructure and moderating
content for illegal activities), it occurred to me that Reddit is as a "domain"
is something people would understand without any training, while its
implementation could (kinda) justify using a lot of scalability techniques.
I'm writing _kinda_ because most people if they wanted to set up their own
instance, would be much better off with one of many projects
[available on GitHub](https://github.com/search?q=Reddit+clone).

Therefore, I started implementing my own Reddit-clone which would use many
techniques allowing scalability, that I learned over time. We are assuming
that they would be justified, even though in a real life project you _should_
have some numbers and proofs justifying their usage.

While I cannot claim that I implemented them in the best way possible
(I certainly didn't) it could at least serve as some point of reference
for people who are truly clueless about where to even start.

## Goals

And so the goals for this project were set:

 * implementing basing functionality of a Reddit-clone, that is:
   * registration
   * subscribing to channels
   * creating/editing/deleting channels, posts, comments, replies to comments
   * banning users and moderating their content by moderators
 * using Command-Query Responsibility Separation and Event Sourcing to work
   with separated read and write models
 * management of user's data should be compliant with General Data Protection
   Directive (*):
     * users' data should be not logged
     * events and commands containing users' data should be encrypted
     * users' data deletion should be possible without breaking anything
       (user can be safely removed from read models, same as key encrypting
       its personal data in events; projections should handle unencryptable
       users' data)
 * making project easy to run locally and following some good practices:
   * using reliable libraries instead of implementing things myself where
     possible (definition of reliable and possible is subjective)
   * making application fully configurable
   * documenting the API
   * having tests which doesn't depend on implementation
   * using linters to detect issues
   * using CI during development
   * obviously a set of good practices is subjective, so certain decisions
     could be at least controversial for some people e.g. not relying on
     automatic derivation as a rule of thumb

(*) - GDPR is a bit more than that, so don't treat it as an exhaustive check
list and consult your lawyer.

## Possible goals

These goals were good to have, so I consider project done without reaching
any of these, but I consider getting back to it somewhere in the future
to work on them:

 * implementing more Reddit functionalities
   * notifications about received replies, posts and comments being moderated,
     user being banned/unbanned
   * full context search
   * OAuth2 support
 * if some library didn't support some functionality, and I had to extend it
   myself, considering contributing to the original library
 * setting up monitoring and instrumentation services
 * setting up Gatling sets in some non-local environment (benchmarks of
   a scalable app, where app's instance, databases and test suite run on
   the same machine isn't worth much)

## Non-goals

These would be critical if this was to be used as a real world app, but
they would introduce _a lot of_ complexity that would not be about
the application _itself_ but about integrations with some arbitrary
services, so they are harder to justify in a demo app:

 * validating an email - to truly test it, tests would have to be performed
   against some live email server, or they would require a lot of mocking
   (which I don't consider to be a reliable test)
 * detecting illegal content - if you create a free, publicly available
   forum that should scale to a lot of users, there is a great chance that
   it will quickly become overrun with spammers, torrents, CP, threats and
   what not. If you don't remove/ban them, you will be liable for all
   the damage they've done, so you would need an army of mods and/or some
   tools which could direct their attention where such activities happen,
   perhaps event banning things automatically. This is a huge task, vastly
   exceeding creating _just a non-linear discussion site_.
