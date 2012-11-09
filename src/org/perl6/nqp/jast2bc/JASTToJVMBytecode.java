package org.perl6.nqp.jast2bc;

import java.util.*;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import org.apache.bcel.generic.*;

import com.sun.org.apache.bcel.internal.Constants;

public class JASTToJVMBytecode {
	public static void main(String[] argv)
	{
		if (argv.length != 2)
			usage();
		
		try
		{
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(argv[0])));
			ClassGen c = buildClassFrom(in);
			in.close();
			c.getJavaClass().dump(argv[1]);
		}
		catch (Exception e)
		{
			System.err.println("Error: " + e.getMessage());
		}
		
	}
	
	private static ClassGen buildClassFrom(BufferedReader in) throws Exception
	{
		// Read in class name and superclass.
		String curLine, className = null, superName = null;
		while ((curLine = in.readLine()) != null)
		{
			if (curLine.startsWith("+ class "))
			{
				className = curLine.substring("+ class ".length());
			}
			else if (curLine.startsWith("+ super "))
			{
				superName = curLine.substring("+ super ".length());
			}
			else if (curLine.equals("+ method"))
			{
				break;
			}
			else
			{
				throw new Exception("Cannot understand '" + curLine + "'");
			}
		}
		if (className == null)
			throw new Exception("Missing class name");
		if (superName == null)
			throw new Exception("Missing superclass name");
		
		// Create class generator object.
		ClassGen c = new ClassGen(className, superName,  "<generated>",
				Constants.ACC_PUBLIC | Constants.ACC_SUPER, null);
		ConstantPoolGen cp = c.getConstantPool();
		InstructionList il = new InstructionList();
		
		// Process all of the methods.
		if (!curLine.equals("+ method"))
			throw new Exception("Expected method after class configuration");
		while (processMethod(in, c, cp, il))
			;
		
		return c;
	}
	
	private static boolean processMethod(BufferedReader in, ClassGen c,
			ConstantPoolGen cp, InstructionList il) throws Exception {
		String curLine, methodName = null, returnType = null;
		boolean isStatic = false;
		List<String> argNames = new ArrayList<String>();
		List<Type> argTypes = new ArrayList<Type>();
		Map<String, InstructionHandle> labelIns = new HashMap<String, InstructionHandle>();
		Map<String, ArrayList<BranchInstruction>> labelFixups = new HashMap<String, ArrayList<BranchInstruction>>();
		
		MethodGen m = null;
		InstructionFactory f = null;
		
		boolean inMethodHeader = true;
		while ((curLine = in.readLine()) != null) {
			// See if we need to move to the next method.
			if (curLine.equals("+ method")) {
				if (inMethodHeader)
					throw new Exception("Unexpected + method in method header");
				finishMethod(c, cp, il, m, labelIns, labelFixups);
				return true;
			}
			
			// Is it a header line?
			if (curLine.startsWith("++ ")) {
				if (!inMethodHeader)
					throw new Exception("Unexpected method header directive: " + curLine);
				if (curLine.startsWith("++ name "))
					methodName = curLine.substring("++ name ".length());
				else if (curLine.startsWith("++ returns "))
					returnType = curLine.substring("++ returns ".length());
				else if (curLine.equals("++ static"))
					isStatic = true;
				else if (curLine.startsWith("++ arg ")) {
					String[] bits = curLine.split("\\s", 4);
					argNames.add(bits[2]);
					argTypes.add(processType(bits[3]));
				}
				else
					throw new Exception("Cannot understand '" + curLine + "'");
				continue;
			}
			
			// Otherwise, we have an instruction. If we've been in the method
			// header, this will be the first instruction also.
			if (inMethodHeader) {
				// Transition to instructions mode.
				inMethodHeader = false;
				
				// Create method object.
				m = new MethodGen(
						(isStatic
							? Constants.ACC_STATIC | Constants.ACC_PUBLIC
							: Constants.ACC_PUBLIC),
						processType(returnType),
						argTypes.toArray(new Type[0]),
						argNames.toArray(new String[0]),
						methodName, c.getClassName(),
						il, cp);
				 f = new InstructionFactory(c);
			}
			
			// Check if it's a label.
			if (curLine.startsWith(":")) {
				String labelName = curLine.substring(1);
				if (labelIns.containsKey(labelName))
					throw new Exception("Duplicate label: " + labelName);
				labelIns.put(labelName, il.getEnd());
				continue;
			}
			
			// Process line as an instruction.
			emitInstruction(il, f, labelFixups, curLine);
		}
		if (inMethodHeader)
			throw new Exception("Unexpected end of file in method header");
		finishMethod(c, cp, il, m, labelIns, labelFixups);
		return false;
	}

	private static void emitInstruction(InstructionList il, InstructionFactory f,
			Map<String, ArrayList<BranchInstruction>> labelFixups,
			String curLine) throws Exception {
		// Find instruciton code and get rest of the string.
		int endIns = curLine.indexOf(" ");
		String rest = "";
		if (endIns < 0)
			endIns = curLine.length();
		else
			rest = curLine.substring(endIns + 1);
		int instruction = Integer.parseInt(curLine.substring(0, endIns));
		
		// Go by instruction.
		switch (instruction) {
		case 0x00: // nop
			il.append(InstructionConstants.NOP);
			break;
		case 0x01: //aconst_null
			il.append(InstructionConstants.ACONST_NULL);
			break;
		case 0x02: // iconst_m1
			il.append(InstructionConstants.ICONST_M1);
			break;
		case 0x03: // iconst_0
			il.append(InstructionConstants.ICONST_0);
			break;
		case 0x04: // iconst_1
			il.append(InstructionConstants.ICONST_1);
			break;
		case 0x05: // iconst_2
			il.append(InstructionConstants.ICONST_2);
			break;
		case 0x06: // iconst_3
			il.append(InstructionConstants.ICONST_3);
			break;
		case 0x07: // iconst_4
			il.append(InstructionConstants.ICONST_4);
			break;
		case 0x08: // iconst_5
			il.append(InstructionConstants.ICONST_5);
			break;
		case 0x09: // lconst_0
			il.append(InstructionConstants.LCONST_0);
			break;
		case 0x0a: // lconst_1
			il.append(InstructionConstants.LCONST_1);
			break;
		case 0x0b: // fconst_0
			il.append(InstructionConstants.FCONST_0);
			break;
		case 0x0c: // fconst_1
			il.append(InstructionConstants.FCONST_1);
			break;
		case 0x0d: // fconst_2
			il.append(InstructionConstants.FCONST_2);
			break;
		case 0x0e: // dconst_0
			il.append(InstructionConstants.DCONST_0);
			break;
		case 0x0f: // dconst_1
			il.append(InstructionConstants.DCONST_1);
			break;
		case 0x1a: // iload_0
			il.append(InstructionFactory.createLoad(Type.INT, 0));
			break;
		case 0x1b: // iload_1
			il.append(InstructionFactory.createLoad(Type.INT, 1));
			break;
		case 0x1c: // iload_2
			il.append(InstructionFactory.createLoad(Type.INT, 2));
			break;
		case 0x1d: // iload_3
			il.append(InstructionFactory.createLoad(Type.INT, 3));
			break;
		case 0x1e: // lload_0
			il.append(InstructionFactory.createLoad(Type.LONG, 0));
			break;
		case 0x1f: // lload_1
			il.append(InstructionFactory.createLoad(Type.LONG, 1));
			break;
		case 0x20: // lload_2
			il.append(InstructionFactory.createLoad(Type.LONG, 2));
			break;
		case 0x21: // lload_3
			il.append(InstructionFactory.createLoad(Type.LONG, 3));
			break;
		case 0x22: // fload_0
			il.append(InstructionFactory.createLoad(Type.FLOAT, 0));
			break;
		case 0x23: // fload_1
			il.append(InstructionFactory.createLoad(Type.FLOAT, 1));
			break;
		case 0x24: // fload_2
			il.append(InstructionFactory.createLoad(Type.FLOAT, 2));
			break;
		case 0x25: // fload_3
			il.append(InstructionFactory.createLoad(Type.FLOAT, 3));
			break;
		case 0x26: // dload_0
			il.append(InstructionFactory.createLoad(Type.DOUBLE, 0));
			break;
		case 0x27: // dload_1
			il.append(InstructionFactory.createLoad(Type.DOUBLE, 1));
			break;
		case 0x28: // dload_2
			il.append(InstructionFactory.createLoad(Type.DOUBLE, 2));
			break;
		case 0x29: // dload_3
			il.append(InstructionFactory.createLoad(Type.DOUBLE, 3));
			break;
		case 0x2a: // aload_0
			il.append(InstructionFactory.createLoad(Type.OBJECT, 0));
			break;
		case 0x2b: // aload_1
			il.append(InstructionFactory.createLoad(Type.OBJECT, 1));
			break;
		case 0x2c: // aload_2
			il.append(InstructionFactory.createLoad(Type.OBJECT, 2));
			break;
		case 0x2d: // aload_3
			il.append(InstructionFactory.createLoad(Type.OBJECT, 3));
			break;
		case 0x60: // iadd
			il.append(InstructionConstants.IADD);
			break;
		case 0x61: // ladd
			il.append(InstructionConstants.LADD);
			break;
		case 0x62: // fadd
			il.append(InstructionConstants.FADD);
			break;
		case 0x63: // dadd
			il.append(InstructionConstants.DADD);
			break;
		case 0x64: // isub
			il.append(InstructionConstants.ISUB);
			break;
		case 0x65: // lsub
			il.append(InstructionConstants.LSUB);
			break;
		case 0x66: // fsub
			il.append(InstructionConstants.FSUB);
			break;
		case 0x67: // dsub
			il.append(InstructionConstants.DSUB);
			break;
		case 0x68: // imul
			il.append(InstructionConstants.IMUL);
			break;
		case 0x69: // lmul
			il.append(InstructionConstants.LMUL);
			break;
		case 0x6a: // fmul
			il.append(InstructionConstants.FMUL);
			break;
		case 0x6b: // dmul
			il.append(InstructionConstants.DMUL);
			break;
		case 0x6c: // idiv
			il.append(InstructionConstants.IDIV);
			break;
		case 0x6d: // ldiv
			il.append(InstructionConstants.LDIV);
			break;
		case 0x6e: // fdiv
			il.append(InstructionConstants.FDIV);
			break;
		case 0x6f: // ddiv
			il.append(InstructionConstants.DDIV);
			break;
		case 0x70: // irem
			il.append(InstructionConstants.IREM);
			break;
		case 0x71: // lrem
			il.append(InstructionConstants.LREM);
			break;
		case 0x72: // frem
			il.append(InstructionConstants.FREM);
			break;
		case 0x73: // drem
			il.append(InstructionConstants.DREM);
			break;
		case 0x74: // ineg
			il.append(InstructionConstants.INEG);
			break;
		case 0x75: // lneg
			il.append(InstructionConstants.LNEG);
			break;
		case 0x76: // fneg
			il.append(InstructionConstants.FNEG);
			break;
		case 0x77: // dneg
			il.append(InstructionConstants.DNEG);
			break;
		case 0x78: // ishl
			il.append(InstructionConstants.ISHL);
			break;
		case 0x79: // lshl
			il.append(InstructionConstants.LSHL);
			break;
		case 0x7a: // ishr
			il.append(InstructionConstants.ISHR);
			break;
		case 0x7b: // lshr
			il.append(InstructionConstants.LSHR);
			break;
		case 0x7c: // iushr
			il.append(InstructionConstants.IUSHR);
			break;
		case 0x7d: // lushr
			il.append(InstructionConstants.LUSHR);
			break;
		case 0x7e: // iand
			il.append(InstructionConstants.IAND);
			break;
		case 0x7f: // land
			il.append(InstructionConstants.LAND);
			break;
		case 0x80: // ior
			il.append(InstructionConstants.IOR);
			break;
		case 0x81: // lor
			il.append(InstructionConstants.LOR);
			break;
		case 0x82: // ixor
			il.append(InstructionConstants.IXOR);
			break;
		case 0x83: // lxor
			il.append(InstructionConstants.LXOR);
			break;
		case 0x85: // i2l
			il.append(InstructionConstants.I2L);
			break;
		case 0x86: // i2f
			il.append(InstructionConstants.I2F);
			break;
		case 0x87: // i2d
			il.append(InstructionConstants.I2D);
			break;
		case 0x88: // l2i
			il.append(InstructionConstants.L2I);
			break;
		case 0x89: // l2f
			il.append(InstructionConstants.L2F);
			break;
		case 0x8a: // l2d
			il.append(InstructionConstants.L2D);
			break;
		case 0x8b: // f2i
			il.append(InstructionConstants.F2I);
			break;
		case 0x8c: // f2l
			il.append(InstructionConstants.F2L);
			break;
		case 0x8d: // f2d
			il.append(InstructionConstants.F2D);
			break;
		case 0x8e: // d2i
			il.append(InstructionConstants.D2I);
			break;
		case 0x8f: // d2l
			il.append(InstructionConstants.D2L);
			break;
		case 0x90: // d2f
			il.append(InstructionConstants.D2F);
			break;
		case 0x91: // i2b
			il.append(InstructionConstants.I2B);
			break;
		case 0x92: // i2c
			il.append(InstructionConstants.I2C);
			break;
		case 0x93: // i2s
			il.append(InstructionConstants.I2S);
			break;
		case 0x94: // lcmp
			il.append(InstructionConstants.LCMP);
			break;
		case 0x95: // fcmpl
			il.append(InstructionConstants.FCMPL);
			break;
		case 0x96: // fcmpg
			il.append(InstructionConstants.FCMPG);
			break;
		case 0x97: // dcmpl
			il.append(InstructionConstants.DCMPL);
			break;
		case 0x98: // dcmpg
			il.append(InstructionConstants.DCMPG);
			break;
		case 0xa7: // goto
			BranchInstruction bi = InstructionFactory.createBranchInstruction((short)0xa7, null);
			if (!labelFixups.containsKey(rest))
				labelFixups.put(rest, new ArrayList<BranchInstruction>());
			labelFixups.get(rest).add(bi);
			il.append(bi);
			break;
		case 0xac: // ireturn
			il.append(InstructionConstants.IRETURN);
			break;
		case 0xad: // lreturn
			il.append(InstructionConstants.LRETURN);
			break;
		case 0xae: // freturn
			il.append(InstructionConstants.FRETURN);
			break;
		case 0xaf: // dreturn
			il.append(InstructionConstants.DRETURN);
			break;
		case 0xb0: // areturn
			il.append(InstructionConstants.ARETURN);
			break;
		case 0xb1: // return
			il.append(InstructionConstants.RETURN);
			break;
		default:
			throw new Exception("Unrecognized instruction line: " + curLine);
		}
	}

	private static void finishMethod(ClassGen c, ConstantPoolGen cp,
			InstructionList il, MethodGen m, Map<String, InstructionHandle> labelIns,
			Map<String, ArrayList<BranchInstruction>> labelFixups) throws Exception {
		// Fix up any labels.
		for (String label : labelFixups.keySet()) {
			if (!labelIns.containsKey(label))
				throw new Exception("Missing label: " + label);
			InstructionHandle target = labelIns.get(label).getNext();
			for (BranchInstruction bi : labelFixups.get(label))
				bi.setTarget(target);
		}
		
		// Finalize method and cleanup instruciton list.
		m.setMaxStack();
		c.addMethod(m.getMethod());
		il.dispose();
	}

	private static Type processType(String typeName) {
		return Type.getType(typeName);
	}

	private static void usage()
	{
		System.err.println("Usage: JASTToJVMBytecode jast-dump-file output-class-file");
		System.exit(1);
	}
}
