package com.scryer.util;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class IdGeneratorTest {

	@Test
	void randomNumberCollisionTest() {
		Set<Long> randoms = new HashSet<>();
		for (int i = 0; i < 1000000; i++) {
			randoms.add(ThreadLocalRandom.current().nextLong());
		}
		System.out.println(randoms.size());
	}

}
