package cats.effect

/** Hack allowing us to access the whole content of cats.effect.IOLocalState */
object IOLocalHack {

  def get: IO[scala.collection.immutable.Map[IOLocal[_], Any]] = IO.Local(state => (state, state))
}
