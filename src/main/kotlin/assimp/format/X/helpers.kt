package assimp.format.X

import assimp.ai_real
import assimp.AiColor4D
import assimp.AiVector2D

class Pointer<T>(var datas: Array<T>, var pointer: Int = 0) {

	//constructor(dat : T, pointer: Int = 0) : this(Array<T>(1, {i -> dat}), pointer)

	var value: T
		get() = datas[pointer]
		set(value) {
			datas[pointer] = value
		}

	val lastIndex: Int get() = datas.lastIndex-pointer

	operator fun inc(): Pointer<T> {
		return Pointer<T>(datas, pointer + 1)
	}

	operator fun dec(): Pointer<T> {
		return Pointer<T>(datas, pointer - 1)
	}

	operator fun plus(b: Int): Pointer<T> {
		return Pointer<T>(datas, pointer + b)
	}

	operator fun minus(b: Int): Pointer<T> {
		return Pointer<T>(datas, pointer - b)
	}

	operator fun compareTo(b: Pointer<T>): Int {
		return pointer - b.pointer
	}

	operator fun get(index: Int): T {
		return datas[pointer+index]
	}

	operator fun set(index: Int, value: T) {
		datas[pointer+index] = value
	}

	fun subset(range: IntRange): MutableList<T> {
		var result: MutableList<T> = mutableListOf()
		for (i in range) {
			result.add(datas[pointer + i])
		}
		return result
	}

}

operator fun Pointer<Char>.get(range: IntRange): String = subset(range).joinToString(separator = "")

fun <T> MutableList<T>.resize(newsize: Int, init: () -> T) {
	if (newsize - size < 0) throw RuntimeException("Downsizing unsupported")
	for (a in size..newsize - 1)
		add(init())
}

fun <T> MutableList<T>.reserve(newsize: Int, init: () -> T) {
	if (newsize - size > 0) {
		for (a in size..newsize - 1)
			add(init())
	}
}

fun String.size(): Int = length
fun StringBuilder.length(): Int = length

fun <T> MutableList<T>.push_back(t: T) = add(t)
fun <T> MutableList<T>.size(): Int = size
fun <T> MutableList<T>.front() = first()
fun <T> MutableList<T>.back() = last()

fun <T> ArrayList<T>.reserve(newsize : Int) = ensureCapacity(newsize)

fun <T> ArrayList<T>.reserve(newsize: Int, init: () -> T) : ArrayList<T> {
    if (newsize - size > 0) {
        for (a in size..newsize - 1)
            add(init())
    }
	return this
}

fun isspace(char: Char): Boolean {
	return char.isWhitespace()
}

fun strncmp(P: Pointer<Char>, s: String, l: Int): Int { //Highly simplified from C++ function definition
	if (P[0..l - 1] == s) return 0
	return -1
}

fun ASSIMP_strincmp(_s1: Pointer<Char>, _s2: String, n: Int): Int {
	return ASSIMP_strincmp(_s1, Pointer<Char>(Array<Char>(_s2.length, { i -> _s2.get(i) })), n)
}

fun ASSIMP_strincmp(_s1: Pointer<Char>, _s2: Pointer<Char>, n: Int): Int {
	var c1: Char;
	var c2: Char;
	var s1 = _s1;
	var s2 = _s2;
	var p: Int = 0;
	do {
		if (p++ >= n) return 0;
		c1 = tolower(s1.value); s1++;
		c2 = tolower(s2.value); s2++;
	} while (c1.toInt() != 0 && (c1 == c2));

	return c1 - c2;
}

fun tolower(c: Char): Char {
	return c.toLowerCase()
}

fun isdigit(c: Char): Boolean {
	return c.isDigit()
}

fun String.length() = length

fun warn(s: String) {
	println(s)
}

fun debug(s : String) {
	println(s)
}



