package org.nutz.el2.arithmetic;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Queue;

import org.nutz.el2.Operator;
import org.nutz.el2.obj.IdentifierObj;
import org.nutz.lang.util.Context;

/**
 * 逆波兰表示法（Reverse Polish notation，RPN，或逆波兰记法），是一种是由波兰数学家扬·武卡谢维奇1920年引入的数学表达式方式，在逆波兰记法中，所有操作符置于操作数的后面，因此也被称为后缀表示法。<br/>
 * 参考:<a href="http://zh.wikipedia.org/wiki/%E9%80%86%E6%B3%A2%E5%85%B0%E8%A1%A8%E7%A4%BA%E6%B3%95">逆波兰表达式</a>
 * 
 * @author juqkai(juqkai@gmail.com)
 *
 */
public class RPNCalculate {
	//存放context
	private final ElCache ec = new ElCache();
	//预编译后的对象
	private Deque<Object> el;
	
	public RPNCalculate() {}
	
	public RPNCalculate(Queue<Object> rpn) {
		el = OperatorTree(rpn);
	}
	
	public Object calculate(Context context){
		ec.setContext(context);
		Deque<Object> el2 = new LinkedList<Object>();
		el2.addAll(el);
		
		if(el2.peek() instanceof Operator){
			Operator obj = (Operator) el2.poll();
			return obj.calculate();
		}
		if(el2.peek() instanceof IdentifierObj){
			return ((IdentifierObj) el2.peek()).fetchVal();
		}
		return el2.poll();
	}
	
	/**
	 * 根据逆波兰表达式进行计算
	 * @param rpn
	 * @return
	 */
	public Object calculate(Context context, Queue<Object> rpn) {
		ec.setContext(context);
		
		Deque<Object> operand = OperatorTree(context, rpn);
		if(operand.peek() instanceof Operator){
			Operator obj = (Operator) operand.poll();
			return obj.calculate();
		}
		if(operand.peek() instanceof IdentifierObj){
			return ((IdentifierObj) operand.peek()).fetchVal();
		}
		return operand.poll();
	}
	/**
	 * 转换成操作树
	 * @param rpn
	 * @return
	 */
	private Deque<Object> OperatorTree(Queue<Object> rpn){
		Deque<Object> operand = new LinkedList<Object>();
		while(!rpn.isEmpty()){
			if(rpn.peek() instanceof Operator){
				Operator opt = (Operator) rpn.poll();
				opt.wrap(operand);
				operand.addFirst(opt);
				continue;
			}
			if(rpn.peek() instanceof IdentifierObj){
//				((IdentifierObj) rpn.peek()).setContext(context);
				((IdentifierObj) rpn.peek()).setEc(ec);
			}
			operand.addFirst(rpn.poll());
		}
		return operand;
	}
	/**
	 * 转换成操作树
	 * @param rpn
	 * @return
	 */
	private Deque<Object> OperatorTree(Context context, Queue<Object> rpn){
		Deque<Object> operand = new LinkedList<Object>();
		while(!rpn.isEmpty()){
			if(rpn.peek() instanceof Operator){
				Operator opt = (Operator) rpn.poll();
				opt.wrap(operand);
				operand.addFirst(opt);
				continue;
			}
			if(rpn.peek() instanceof IdentifierObj){
//				((IdentifierObj) rpn.peek()).setContext(context);
				((IdentifierObj) rpn.peek()).setEc(ec);
			}
			operand.addFirst(rpn.poll());
		}
		return operand;
	}
	

}
