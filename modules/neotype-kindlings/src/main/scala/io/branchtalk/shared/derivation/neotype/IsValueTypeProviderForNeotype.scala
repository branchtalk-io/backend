package io.branchtalk.shared.derivation.neotype

import hearth.fp.data.NonEmptyList
import hearth.{ MacroCommons, MacroCommonsScala3 }
import hearth.std.{ ProviderResult, StandardMacroExtension, StdExtensions }

/** Teaches every Kindlings derivation (avro, jsoniter, sconfig, cats, ...) to treat a neotype opaque type as a value
  * type: unwrap to the underlying and re-wrap by identity (an opaque type has the same representation as its underlying
  * at runtime). Covers all four flavours in use here: neotype's `Newtype`/`Subtype` and branchtalk's higher-kinded
  * `NewtypeT`/`NewtypeF` (see `neotype.newtypes`).
  *
  * This replaces the per-companion `given AvroCodec[X] = AvroSupport.newtypeCodec` boilerplate. neotype exposes its
  * `Newtype.WithType` witness as a `transparent inline given`, which is invisible to the macro-time `Expr.summon` that
  * Kindlings' "use implicit when available" rule performs - so the generic `given [A, B](using Newtype.WithType[B, A],
  * ...)` fallback resolves at a normal call site but never inside the derivation macro. A value-type provider sidesteps
  * implicit search entirely by matching the type structurally at macro-expansion time.
  *
  * Registered via `META-INF/services/hearth.std.StandardMacroExtension`; loaded by Hearth's ServiceLoader whenever a
  * downstream module (which has this one on its compile/macro classpath) expands a Kindlings derivation.
  */
final class IsValueTypeProviderForNeotype extends StandardMacroExtension { loader =>

  override def priority: Int = 1000

  // Base classes of every neotype flavour used in this project (neotype's own + branchtalk's higher-kinded ones).
  private val neotypeBaseNames =
    Set("neotype.Newtype", "neotype.Subtype", "neotype.NewtypeT", "neotype.NewtypeF")

  override def extend(ctx: MacroCommons & StdExtensions): Unit = ctx match {
    case ctx3: (MacroCommonsScala3 & StdExtensions) =>
      import ctx3.{ *, given }
      import ctx3.quotes.reflect.*

      // If `repr` is a neotype opaque type, return its underlying type (via the compiler's translucent super type,
      // which sees through opacity and applied parameterised opaques from any scope); otherwise None. The opaque `Type`
      // member (`Type` for Newtype/Subtype, `Type[_]` for NewtypeT/NewtypeF) is *declared in* the neotype base class,
      // so its owner's fully-qualified name identifies the flavour.
      def neotypeUnderlying(repr: TypeRepr): Option[TypeRepr] = {
        val dealiased = repr.dealias
        val sym       = dealiased.typeSymbol
        if (!sym.flags.is(Flags.Opaque)) None
        else if (!neotypeBaseNames(sym.owner.fullName)) None
        else UntypedType.opaqueUnderlyingType(dealiased)
      }

      IsValueType.registerProvider(
        new IsValueType.Provider {

          override def name: String = loader.getClass.getName

          // Cheap sound pre-filter: every neotype Type is an opaque type, so a non-opaque type can never match.
          override def mightMatch[A](tpe: Type[A]): Boolean = tpe.isOpaqueType

          override def parse[A](tpe: Type[A]): ProviderResult[IsValueType[A]] = {
            implicit val A: Type[A]  = tpe
            val repr:       TypeRepr = UntypedType.fromTyped[A]
            neotypeUnderlying(repr) match {
              case None =>
                skippedLazily(s"${tpe.prettyPrint} is not a neotype Newtype/Subtype/NewtypeT/NewtypeF")
              case Some(innerRepr) =>
                val inner = (innerRepr: UntypedType).as_??
                import inner.Underlying as Inner

                val unwrapExpr: Expr[A] => Expr[Inner] =
                  outer => Expr.quote(Expr.splice(outer).asInstanceOf[Inner])

                // Opaque = identity at runtime, so re-wrapping is a plain cast (no validation on the decode path, matching
                // the old `AvroSupport.newtypeCodec` which only validated when the underlying `make` was called).
                val plainCtor = CtorLikeOf.PlainValue[Inner, A](
                  ctor = innerExpr => Expr.quote(Expr.splice(innerExpr).asInstanceOf[A]),
                  method = None
                )

                ProviderResult.Matched(
                  Existential[IsValueTypeOf[A, _], Inner](
                    new IsValueTypeOf[A, Inner] {
                      override val unwrap: Expr[A] => Expr[Inner] = unwrapExpr
                      override val wrap:   CtorLikeOf[Inner, A]   = plainCtor
                      override lazy val ctors: CtorLikes[A] =
                        NonEmptyList.one(Existential[CtorLikeOf[_, A], Inner](plainCtor))
                    }
                  )
                )
            }
          }
        }
      )
    case _ => ()
  }
}
