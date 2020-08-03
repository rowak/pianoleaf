package io.github.rowak.pianoleaf;

import org.junit.Test;

public class AppTest 
{
    @Test
    public void shouldAnswerWith60() {
    	System.out.println(App.noteToPitch("C4"));
    	System.out.println(App.noteToPitch("A0"));
    	System.out.println(App.noteToPitch("C8"));
    }
}
