// NativeImage-specific case classes for CSV parsing

case class NativeImageMethod
(
  id: Int,
  name: String,
  declaringClass: String,
  parameterTypes: String,
  returnType: String,
  display: String,
  flags: String,
  isEntryPoint: Boolean
)

case class NativeImageInvocation
(
  id: Int,
  methodId: Int,
  bytecodeIndexes: String,
  targetId: Int,
  isDirect: Boolean,
  lineNumber: Int
)

case class NativeImageTarget
(
  invokeId: Int,
  targetId: Int
)