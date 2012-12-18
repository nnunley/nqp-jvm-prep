package org.perl6.nqp.sixmodel.reprs;

import java.util.HashMap;
import java.util.List;
import org.perl6.nqp.sixmodel.*;

public class KnowHOWREPRInstance extends SixModelObject {
	public String name;
	public List<SixModelObject> attributes;
	public HashMap<String, SixModelObject> methods;
}