package io.branchtalk.shared.model

// doesn't rely on macros like shapeless.IsTuple, so it works in IDE
sealed class IsTuple[A]
object IsTuple {

  private val impl: IsTuple[Nothing] = new IsTuple[Nothing]

  implicit def tuple2[A1, A2]: IsTuple[(A1, A2)] =
    impl.asInstanceOf[IsTuple[(A1, A2)]]
  implicit def tuple3[A1, A2, A3]: IsTuple[(A1, A2, A3)] =
    impl.asInstanceOf[IsTuple[(A1, A2, A3)]]
  implicit def tuple4[A1, A2, A3, A4]: IsTuple[(A1, A2, A3, A4)] =
    impl.asInstanceOf[IsTuple[(A1, A2, A3, A4)]]
  implicit def tuple5[A1, A2, A3, A4, A5]: IsTuple[(A1, A2, A3, A4, A5)] =
    impl.asInstanceOf[IsTuple[(A1, A2, A3, A4, A5)]]
  implicit def tuple6[A1, A2, A3, A4, A5, A6]: IsTuple[(A1, A2, A3, A4, A5, A6)] =
    impl.asInstanceOf[IsTuple[(A1, A2, A3, A4, A5, A6)]]
  implicit def tuple7[A1, A2, A3, A4, A5, A6, A7]: IsTuple[(A1, A2, A3, A4, A5, A6, A7)] =
    impl.asInstanceOf[IsTuple[(A1, A2, A3, A4, A5, A6, A7)]]
  implicit def tuple8[A1, A2, A3, A4, A5, A6, A7, A8]: IsTuple[(A1, A2, A3, A4, A5, A6, A7, A8)] =
    impl.asInstanceOf[IsTuple[(A1, A2, A3, A4, A5, A6, A7, A8)]]
  implicit def tuple9[A1, A2, A3, A4, A5, A6, A7, A8, A9]: IsTuple[(A1, A2, A3, A4, A5, A6, A7, A8, A9)] =
    impl.asInstanceOf[IsTuple[(A1, A2, A3, A4, A5, A6, A7, A8, A9)]]
  implicit def tuple10[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10]: IsTuple[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10)] =
    impl.asInstanceOf[IsTuple[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10)]]
  implicit def tuple11[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11]: IsTuple[
    (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11)
  ] =
    impl.asInstanceOf[IsTuple[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11)]]
  implicit def tuple12[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12]: IsTuple[
    (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12)
  ] =
    impl.asInstanceOf[IsTuple[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12)]]
  implicit def tuple13[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13]: IsTuple[
    (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13)
  ] =
    impl.asInstanceOf[IsTuple[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13)]]
  implicit def tuple14[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14]: IsTuple[
    (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14)
  ] =
    impl.asInstanceOf[IsTuple[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14)]]
  implicit def tuple15[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15]: IsTuple[
    (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15)
  ] =
    impl.asInstanceOf[IsTuple[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15)]]
  implicit def tuple16[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16]: IsTuple[
    (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16)
  ] =
    impl.asInstanceOf[IsTuple[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16)]]
  implicit def tuple17[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17]: IsTuple[
    (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17)
  ] =
    impl.asInstanceOf[IsTuple[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17)]]
  implicit def tuple18[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18]: IsTuple[
    (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18)
  ] =
    impl.asInstanceOf[IsTuple[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18)]]
  implicit def tuple19[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19]: IsTuple[
    (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19)
  ] =
    impl.asInstanceOf[IsTuple[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19)]]
  implicit def tuple20[
    A1,
    A2,
    A3,
    A4,
    A5,
    A6,
    A7,
    A8,
    A9,
    A10,
    A11,
    A12,
    A13,
    A14,
    A15,
    A16,
    A17,
    A18,
    A19,
    A20
  ]: IsTuple[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20)] =
    impl.asInstanceOf[IsTuple[
      (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20)
    ]]
  implicit def tuple21[
    A1,
    A2,
    A3,
    A4,
    A5,
    A6,
    A7,
    A8,
    A9,
    A10,
    A11,
    A12,
    A13,
    A14,
    A15,
    A16,
    A17,
    A18,
    A19,
    A20,
    A21
  ]: IsTuple[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21)] =
    impl.asInstanceOf[IsTuple[
      (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21)
    ]]
  implicit def tuple22[
    A1,
    A2,
    A3,
    A4,
    A5,
    A6,
    A7,
    A8,
    A9,
    A10,
    A11,
    A12,
    A13,
    A14,
    A15,
    A16,
    A17,
    A18,
    A19,
    A20,
    A21,
    A22
  ]: IsTuple[(A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22)] =
    impl.asInstanceOf[IsTuple[
      (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22)
    ]]
}
