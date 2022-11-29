
import gov.nasa.jpf.jvm.JVMStackFrame;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.Types;
import gov.nasa.jpf.vm.VM;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gov.nasa.jpf.ListenerAdapter;
import gov.nasa.jpf.jvm.bytecode.DRETURN;
import gov.nasa.jpf.jvm.bytecode.FRETURN;
import gov.nasa.jpf.jvm.bytecode.INSTANCEOF;
import gov.nasa.jpf.jvm.bytecode.INVOKESPECIAL;
import gov.nasa.jpf.jvm.bytecode.INVOKESTATIC;
import gov.nasa.jpf.jvm.bytecode.IRETURN;
import gov.nasa.jpf.jvm.bytecode.JVMReturnInstruction;
import gov.nasa.jpf.jvm.bytecode.LRETURN;

public class MemoizationListener extends ListenerAdapter {

	Map<String, Object> memoizeMethod = new HashMap<String, Object>();

	List<String> primitiveDataType = new ArrayList<String>(
			Arrays.asList("int", "byte", "short", "long", "float", "double", "boolean", "char"));

	String argValue = "";
	String objArgValue = "";
	
	@Override
	public void executeInstruction(VM vm, ThreadInfo currentThread, Instruction instructionToExecute) {

		String fileLocation = instructionToExecute.getFileLocation();
		MethodInfo mi = currentThread.getTopFrameMethodInfo();
		if (fileLocation != null 
				&& !fileLocation.startsWith("java/") 
				&& !fileLocation.startsWith("sun/")
				&& !fileLocation.startsWith("gov/")) {

			System.out.println("CURRENT INSTRUCTION - Line= "
					+ instructionToExecute.getMethodInfo().getLineNumber(instructionToExecute) + ", "
					+ instructionToExecute.toString() + ", InstructionIndex()="
					+ instructionToExecute.getInstructionIndex() );


			if (instructionToExecute instanceof INVOKESTATIC) {

				INVOKESTATIC inst = (INVOKESTATIC) instructionToExecute;

				System.out.println("INVOKESTATIC - instructionToExecute - getInvokedMethodSignature= " + inst.getInvokedMethodSignature());
				System.out.println("INVOKESTATIC - instructionToExecute - getReturnTypeName= " + Types.getReturnTypeName(inst.getInvokedMethodSignature()));
				

				if (primitiveDataType.contains(Types.getReturnTypeName(inst.getInvokedMethodSignature()))) {
					String key = inst.getInvokedMethod().getFullName();

					if (argValue == null || argValue.equals("")) {
						Object[] argVal = inst.getArgumentValues(currentThread);
						Object[] argtype =  inst.getInvokedMethod().getArgumentTypeNames();

						for (int i = 0; i < argVal.length; i++) {
							System.out.println("INVOKESTATIC - argType[" + i + "]= " + argtype[i]+" , argvalue[" + i + "]= " + argVal[i]);
							
							if(primitiveDataType.contains(argtype[i]))							
								argValue += "_" + argVal[i];						
						}					
					} 
					
					if(!argValue.equals("") || !objArgValue.equals("")) {
						key = key + argValue + objArgValue;
					}
					
					System.out.println("INVOKESTATIC - argValue= " + argValue);
					System.out.println("INVOKESTATIC - objArgValue= " + objArgValue);
					System.out.println("INVOKESTATIC - key= " + key);
					
					if (memoizeMethod.get(key) == null) {
						memoizeMethod.put(key, "");
						System.out.println("INVOKESTATIC - method with arguments used first time");
					} 
					else {
						System.out.println("INVOKESTATIC - method with arguments already computed, skipping further computation");
						System.out.println("INVOKESTATIC - executing ireturn , key= " + key +", value= " + memoizeMethod.get(key));

						int[] slots = ((JVMStackFrame) ((List<Object>) memoizeMethod.get(key)).get(1)).getSlots();
						for (int i = 0; i < slots.length - 2; i++) {

							System.out.println("INVOKESTATIC - Modifiable Frame peek = "
									+ currentThread.getModifiableLastNonSyntheticStackFrame().peek());
							System.out.println("INVOKESTATIC - Modifiable Frame pop = "
									+ currentThread.getModifiableLastNonSyntheticStackFrame().pop());
						}

						if( ((List<Object>) memoizeMethod.get(key)).get(0) instanceof Integer) {
							currentThread.getModifiableLastNonSyntheticStackFrame()
							.push((Integer) ((List<Object>) memoizeMethod.get(key)).get(0));
						}
						else if ( ((List<Object>) memoizeMethod.get(key)).get(0) instanceof Long) {
							currentThread.getModifiableLastNonSyntheticStackFrame()
							.pushLong((Long) ((List<Object>) memoizeMethod.get(key)).get(0));
						}
						else if ( ((List<Object>) memoizeMethod.get(key)).get(0) instanceof Float) {
							currentThread.getModifiableLastNonSyntheticStackFrame()
							.pushFloat((Float) ((List<Object>) memoizeMethod.get(key)).get(0));
						}

						System.out.println("INVOKESTATIC - Modifiable Frame  = "
								+ currentThread.getModifiableLastNonSyntheticStackFrame());

						IRETURN iReturn = new IRETURN();
						Instruction instruction = mi.getInstruction(instructionToExecute.getInstructionIndex() + 1);
						iReturn.init(mi, instruction.getInstructionIndex(), instruction.getPosition());

						currentThread.setPC(iReturn);
						if (instructionToExecute.getNext() != null)
							currentThread.setNextPC(instruction);

						argValue="";
						objArgValue="";
					}

				}
			}

			if (instructionToExecute instanceof INVOKESPECIAL && fileLocation != null
					&& !fileLocation.startsWith("java.") && !fileLocation.startsWith("sun.")
					&& !fileLocation.startsWith("gov.")) {
					
				INVOKESPECIAL inst = (INVOKESPECIAL) instructionToExecute;
				String methodClassName = inst.getInvokedMethodClassName();
				
				if (methodClassName != null && !methodClassName.startsWith("java.")
						&& !methodClassName.startsWith("sun.") && !methodClassName.startsWith("gov.")) {

					
					System.out.println("INVOKESPECIAL - instructionToExecute - getInvokedMethodSignature= "+ inst.getInvokedMethodSignature());
					System.out.println("INVOKESPECIAL - instructionToExecute - getReturnTypeName= "+ Types.getReturnTypeName(inst.getInvokedMethodSignature()));

					if (objArgValue == null || objArgValue.equals("")) {
						
						Object[] argVal = inst.getArgumentValues(currentThread);
						Object[] argtype =  inst.getInvokedMethod().getArgumentTypeNames();

						for (int i = 0; i < argVal.length; i++) {
							System.out.println("INVOKESPECIAL - argType[" + i + "]= " + argtype[i]+" , argvalue[" + i + "]= " + argVal[i]);
							
							if(primitiveDataType.contains(argtype[i]))
							{						
								objArgValue += "_" + argVal[i];
							}
						}
					}
					System.out.println("INVOKESPECIAL - objArgValue= " + objArgValue);
				}
			}
		}

	}

	@Override
	public void instructionExecuted(VM vm, ThreadInfo currentThread, Instruction nextInstruction,
			Instruction executedInstruction) {
		String fileLocation = executedInstruction.getFileLocation();
		if (fileLocation != null && !fileLocation.startsWith("java/") && !fileLocation.startsWith("sun/")
				&& !fileLocation.startsWith("gov/")) {
			JVMReturnInstruction returnInst = null;
			if (executedInstruction instanceof IRETURN) {
				returnInst = (IRETURN) executedInstruction;
			}else if (executedInstruction instanceof LRETURN){
				returnInst = (LRETURN) executedInstruction;
			}else if (executedInstruction instanceof DRETURN){
				returnInst = (DRETURN) executedInstruction;
			}else if (executedInstruction instanceof FRETURN){
				returnInst = (FRETURN) executedInstruction;
			}
			
				if(executedInstruction instanceof IRETURN 
						|| executedInstruction instanceof LRETURN
						|| executedInstruction instanceof DRETURN
						|| executedInstruction instanceof FRETURN)
			{
				System.out.println("IRETURN - executedInstruction - getMethodInfo = " + executedInstruction.getMethodInfo());
				System.out.println("IRETURN - executedInstruction - getReturnValue = " + returnInst.getReturnValue(currentThread));

				String key = executedInstruction.getMethodInfo().getFullName();
				key += argValue + objArgValue;
				
				System.out.println("IRETURN - key= " + key +" , value= " + memoizeMethod.get(key));
				if (memoizeMethod.get(key) != null) {
					List<Object> obj = new ArrayList<Object>();

					JVMStackFrame frame = (JVMStackFrame) currentThread.getInvokedStackFrames().get(0);
					obj.add(returnInst.getReturnValue(currentThread));
					obj.add(returnInst.getReturnFrame());
					memoizeMethod.put(key, obj);
				}
				argValue="";
				objArgValue="";
			}
			
			 
		}
	}
}
