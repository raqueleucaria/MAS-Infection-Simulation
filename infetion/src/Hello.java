import jade.core.Agent;

public class Hello  extends Agent {
	private static final long serialVersionUID = 1L;
	protected void setup() {
		System.out.println("Olá Mundo! ");
		System.out.println("Meu nome: " + getLocalName());
	}

}