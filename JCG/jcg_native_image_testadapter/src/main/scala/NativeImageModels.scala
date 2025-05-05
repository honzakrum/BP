/**
 * Case classes for parsing and representing Native Image call graph CSV outputs.
 *
 * @author Jan Křůmal
 */
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