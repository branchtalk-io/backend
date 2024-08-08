package io.branchtalk.shared.model

extension [I1, I2, Out](f: ((I1, I2)) => Out) def untupled(i1: I1, i2: I2): Out = f.apply((i1, i2))

extension [I1, I2, Out](f: (I1, I2) => Out) def untupled(i1: I1, i2: I2): Out = f.apply(i1, i2)

extension [I1, I2, I3, Out](f: ((I1, I2, I3)) => Out) def untupled(i1: I1, i2: I2, i3: I3): Out = f.apply((i1, i2, i3))

extension [I1, I2, I3, Out](f: (I1, (I2, I3)) => Out) def untupled(i1: I1, i2: I2, i3: I3): Out = f.apply(i1, (i2, i3))

extension [I1, I2, I3, I4, Out](f: ((I1, I2, I3, I4)) => Out) {
  def untupled(i1: I1, i2: I2, i3: I3, i4: I4): Out = f.apply((i1, i2, i3, i4))
}

extension [I1, I2, I3, I4, Out](f: (I1, (I2, I3, I4)) => Out) {
  def untupled(i1: I1, i2: I2, i3: I3, i4: I4): Out = f.apply(i1, (i2, i3, i4))
}

extension [I1, I2, I3, I4, I5, Out](f: ((I1, I2, I3, I4, I5)) => Out) {
  def untupled(i1: I1, i2: I2, i3: I3, i4: I4, i5: I5): Out = f.apply((i1, i2, i3, i4, i5))
}

extension [I1, I2, I3, I4, I5, Out](f: (I1, (I2, I3, I4, I5)) => Out) {
  def untupled(i1: I1, i2: I2, i3: I3, i4: I4, i5: I5): Out = f.apply(i1, (i2, i3, i4, i5))
}

extension [I1, I2, I3, I4, I5, I6, Out](f: ((I1, I2, I3, I4, I5, I6)) => Out) {
  def untupled(i1: I1, i2: I2, i3: I3, i4: I4, i5: I5, i6: I6): Out = f.apply((i1, i2, i3, i4, i5, i6))
}

extension [I1, I2, I3, I4, I5, I6, Out](f: (I1, (I2, I3, I4, I5, I6)) => Out) {
  def untupled(i1: I1, i2: I2, i3: I3, i4: I4, i5: I5, i6: I6): Out = f.apply(i1, (i2, i3, i4, i5, i6))
}

extension [I1, I2, I3, I4, I5, I6, I7, Out](f: ((I1, I2, I3, I4, I5, I6, I7)) => Out) {
  def untupled(i1: I1, i2: I2, i3: I3, i4: I4, i5: I5, i6: I6, i7: I7): Out = f.apply((i1, i2, i3, i4, i5, i6, i7))
}

extension [I1, I2, I3, I4, I5, I6, I7, Out](f: (I1, (I2, I3, I4, I5, I6, I7)) => Out) {
  def untupled(i1: I1, i2: I2, i3: I3, i4: I4, i5: I5, i6: I6, i7: I7): Out = f.apply(i1, (i2, i3, i4, i5, i6, i7))
}
