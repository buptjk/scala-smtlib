package smtlib
package interpreters

import lexer.Lexer
import parser.Parser
import parser.Commands._
import parser.CommandsResponses._
import printer._

import java.io._

abstract class ProcessInterpreter extends Interpreter {

  protected val process: Process

  lazy val in = new BufferedWriter(new OutputStreamWriter(process.getOutputStream))
  lazy val out = new BufferedReader(new InputStreamReader(process.getInputStream))

  lazy val parser = new Parser(new Lexer(out))

  override def eval(cmd: Command): CommandResponse = {
    try {
      RecursivePrinter.printCommand(cmd, in)
      in.write("\n")
      in.flush
      cmd match {
        case CheckSat() => parser.parseCheckSatResponse
        case GetAssertions() => parser.parseGetAssertionsResponse
        case GetUnsatCore() => parser.parseGetUnsatCoreResponse
        case GetProof() => parser.parseGetProofResponse
        case GetValue(_, _) => parser.parseGetValueResponse
        case GetAssignment() => parser.parseGetAssignmentResponse

        case GetOption(_) => parser.parseGetOptionResponse
        case GetInfo(_) => parser.parseGetInfoResponse

        case GetModel() => parser.parseGetModelResponse

        case _ => parser.parseGenResponse
      }
    } catch {
      case (ex: Exception) => {
        if(cmd == CheckSat()) CheckSatResponse(UnknownStatus)
        else Error("Solver encountered exception: " + ex)
      }
    }
  }

  override def free(): Unit = {
    RecursivePrinter.printCommand(Exit(), in)
    in.write("\n")
    in.flush

    process.destroy
    in.close
  }

  def kill(): Unit = {
    process.destroy()
  }

  override def interrupt(): Unit = {
    kill()
  }
}
