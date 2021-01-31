# Lessons learned

Actually, some of these lessons I already knew, but they are demonstrable with
this project. Also, some of these are purely subjective, so YMMV and you can
disagree with me.

## Refined and `@newtype`s

Usage of `Refined` together with tagged typed on the same level (I mean
something like `String @@ User.Name with Refined[NonEmpty]`) was a pain, so
this pattern (promoted by Gabriel Volpe) of nesting them instead
(`@newtype case class Username(value: String Refined NonEmpty)`) is a huge
improvement. Using it in domain was an excellent developer experience.

With a fly in the ointment. Refined types are annoying if you don't have
an integration available - though almost all libraries have them, Jsoniter is
a notable exception - while `@newtype`s by their nature prevents having any
integration out of the box - since you just created a new type which should not
act like the type you created it from, derivation is not automatic. You are
encouraged to either use `.coerce` during manual(!) semiauto for each type you
want to generate type class for or creating manually a global implicit that
uses `Coercible` to generate all newtype instances. Hopefully in Dotty
`opaque type`s together with `derriving` with handle that better.

It was particularly painful for Jsoniter which doesn't support generic
implicits for extending its functionality, so there I was virtually forced
to write semiauto instances for every Refined and `@newtype` element of the ADT
even though for any other case Jsoniter would derive whole codec recursively
without any manual interventions. And while we are talking about Jsonier...

## Jsoniter

As long as you are biased towards semiauto AND you aren't heavily using:

 * Refined
 * `@newtype`s
 * `CodecMakerConfig`

Jsoniter works great. Catnip can be configured to derive codecs with macro
annotation which greatly reduces boilerplate. However, the moment you decide
to use any of them, you are falling back on: manually writing semiauto,
implementing support for Refined and `@newtype` on your own, copy-pasting
configs and having a lot of red lines in IDE as compiler-only dependencies
aren't very developer and IDE friendly. I implemented my own `.map` and
`.mapDecode` extension methods, and I have a stron feelings that I invented
something which should have been the part of the core library from the start.

I can definitely see the potential of using Jsoniter as the fastest JSON
library for Scala, but you can see a lot of fields to improve the developer
experience, which would push people towards other libraries, even if they are
not DDoS safe.

If you have a use case for Jsoniter (Circe parser throws exceptions for your
data) you can make things more bearable, but it can require some work and
experience. I know a several cases where Circe Jaws and uJson failed to
process some big JSONs and Jsoniter Scala succeeded.

## Tapir

Even if you don't want to share the same algebra between server, client and
OpenAPI generator I still see the reason to use Tapir. Namely: it has much
better DSL for defining endpoints than Http4s, both vanilla and Rho. I simply
hate both and consider them something more suited to use internally, by some
library interpreted to Http4s rather than by programmers directly.

Tapir already has a lot of interpreters, so once you define your endpoints
you shouldn't need to implement one on your own - I did implement Jsoniter
support for OpenAPI because I already decided on Jsoniter and wanted to avoid
number of JSON libraries in my application (officially there is only Circe
codec support available).

Where I see Tapir's limitations is its definition - you define endpoints using
some ADT, that is sealed traits and case classes, have some methods modifying
these, and at some point run some interpreter which turns them into route,
service call or whatever. Such ADT cannot be extended. You can only wrap it
and provide new functionalities in the wrapper. Endpoint4s let you add
extensions everywhere - you are forced later on to provide interpreters for
these as well, but in the process you can override and extend existing
interpreters. This makes Tapir more straightforward but also more limited
than Endpoint4s in my opinion.

## Tagless-final

I used it in a lot of projects and in every single one of them I both:

 * felt that doing it is somehow aesthetically pleasing
 * using them in application's code is most of the time completely redundant
   optimization towards generic implementation
 * something complicating usage of bifunctors: Cats instances doesn't support
   you `F[_]` being bifunctor, Izumi's BIO hierarchy is a separate project
   and `EitherT[F, E, A]` is (IMHO) a commitment to implementation that
   undermines the point of using tagless final (sorry, but that's my personal
   feelings: if you fixed the way you have 2 params to `EitherT` you might as
   well fix `F` to Cats IO, Monix Task, or ZIO)

I am not convinced about arguments that there is no difference between
application code and library code: my experiences are that they are completely
different: in goals, approaches, consumers. When you don't know who, how and
when will be using your code, you have to work with a little assumptions and
you provide a little guarantees. When you know exactly how your code will be run
you can write in much simpler way. Sometimes you discover that there are more
potential consumer for your code, and then you would have to generalize and
with TTFI you already generalized. But for me it reeks of premature
optimization (towards being generic) and premature abstraction spanning across
your whole application.

I do admit that TTFI code looks pretty to me. But I guess Enterprise FizzBuzz
authors could feel the same about abstracting away from `String` in case they
wanted to use some different `String` implementation. Except they are joking
and we are serious.

## Developer friendliness

Some feedback I got suggest that many (experiences and knowledgeable)
developers still:

 * find it an overkill to use `Refined` outside of config parsing
 * see no reason to use `@newtype` anywhere when they have `AnyVal`
 * hate macro annotations and semiautomatic derivation (I admit I overdo them)
 * find tagless final hard to read and actively developer hostile

therefore, I cannot claim that my project is easy to read by your average
developer, which makes it kinda failure as an example project. `¯\_(ツ)_/¯`
