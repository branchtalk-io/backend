package io.branchtalk.shared.infrastructure

// Allows `import PureconfigSupport._` instead of `import pureconfig._, pureconfig.module.cats._, ...`.
object PureconfigSupport extends LowPriorityPureconfigImplicit {

  export pureconfig.{ ConfigReader, ConfigSource, ConfigWriter }
  export pureconfig.generic.derivation.default.derived

  extension [A](reader: ConfigReader[A]) {
    @SuppressWarnings(Array("org.wartremover.warts.ToString"))
    def emapString[B](tpe: String)(f: A => Either[String, B]): ConfigReader[B] =
      reader.emap(a => f(a).left.map(err => pureconfig.error.CannotConvert(a.toString, tpe, err)))
  }

  // Cats

  export pureconfig.module.cats.{
    lowPriorityNonReducibleReader,
    lowPriorityNonReducibleWriter,
    nonEmptyChainReader,
    nonEmptyChainWriter,
    nonEmptyListReader,
    nonEmptyListWriter,
    nonEmptyMapReader,
    nonEmptyMapWriter,
    nonEmptySetReader,
    nonEmptySetWriter,
    nonEmptyVectorReader,
    nonEmptyVectorWriter
  }

  // enumeratum

  export pureconfig.module.enumeratum.{
    enumeratumByteConfigConvert,
    enumeratumCharConfigConvert,
    enumeratumConfigConvert,
    enumeratumIntConfigConvert,
    enumeratumLongConfigConvert,
    enumeratumShortConfigConvert,
    enumeratumStringConfigConvert
  }
}

// for some reason original traversableReader is not seen, maybe because author forgot to annotate it?
trait LowPriorityPureconfigImplicit extends pureconfig.CollectionReaders
