package assimp.format.X

import assimp.ai_real

val Number.a : ai_real get() = toFloat()

val AI_FAST_ATOF_RELAVANT_DECIMALS : Int = 15

var fast_atof_table : Array<Double> =  arrayOf(  // we write [16] here instead of [] to work around a swig bug
    0.0,
    0.1,
    0.01,
    0.001,
    0.0001,
    0.00001,
    0.000001,
    0.0000001,
    0.00000001,
    0.000000001,
    0.0000000001,
    0.00000000001,
    0.000000000001,
    0.0000000000001,
    0.00000000000001,
    0.000000000000001)

fun fast_atoreal_move(_c : Pointer<Char>, out : Pointer<ai_real>, check_comma : Boolean = true) : Pointer<Char> {
    var f : ai_real = 0.0.a;
	var c = _c
	
    var inv : Boolean = (c.value == '-');
    if (inv || c.value == '+') {
        ++c;
    }

    if ((c[0] == 'N' || c[0] == 'n') && ASSIMP_strincmp(c, "nan", 3) == 0)
    {
        out.value = ai_real.NaN;
        c += 3;
        return c;
    }

    if ((c[0] == 'I' || c[0] == 'i') && ASSIMP_strincmp(c, "inf", 3) == 0)
    {
        out.value = ai_real.POSITIVE_INFINITY;
        if (inv) {
            out.value = ai_real.NEGATIVE_INFINITY //Original code: out.value = -out.value;
        }
        c += 3;
        if ((c[0] == 'I' || c[0] == 'i') && ASSIMP_strincmp(c, "inity", 5) == 0)
        {
            c += 5;
        }
        return c;
    }

    if ((!(c[0] >= '0' && c[0] <= '9')) && (!((c[0] == '.' || (check_comma && c[0] == ','))) && c[1] >= '0' && c[1] <= '9'))
    {
        throw RuntimeException("Cannot parse string "+
                                    "as real number: does not start with digit "+
                                    "or decimal point followed by digit: " + c[0..1]);
    }

    if (c.value != '.' && (! check_comma || c[0] != ','))
    {
        var cp = Pointer<Pointer<Char>>(Array<Pointer<Char>>(1, {c})); f = ( strtoul10_64 (c, cp)).a; c=cp.value
    }

    if ((c.value == '.' || (check_comma && c[0] == ',')) && c[1] >= '0' && c[1] <= '9')
    {
        ++c;

        // NOTE: The original implementation is highly inaccurate here. The precision of a single
        // IEEE 754 float is not high enough, everything behind the 6th digit tends to be more
        // inaccurate than it would need to be. Casting to double seems to solve the problem.
        // strtol_64 is used to prevent integer overflow.

        // Another fix: this tends to become 0 for long numbers if we don't limit the maximum
        // number of digits to be read. AI_FAST_ATOF_RELAVANT_DECIMALS can be a value between
        // 1 and 15.
        var diff = Pointer<Int>(Array<Int>(1,{AI_FAST_ATOF_RELAVANT_DECIMALS}));
		
        var cp = Pointer<Pointer<Char>>(Array<Pointer<Char>>(1, {c}));
		var pl = ( strtoul10_64 ( c, cp, diff )).toDouble();
		c = cp.value

        pl *= fast_atof_table[diff.value];
        f += pl.a;
    }
    // For backwards compatibility: eat trailing dots, but not trailing commas.
    else if (c.value == '.') {
        ++c;
    }

    // A major 'E' must be allowed. Necessary for proper reading of some DXF files.
    // Thanks to Zhao Lei to point out that this if() must be outside the if (c.value == '.' ..)
    if (c.value == 'e' || c.value == 'E') {

        ++c;
        var einv = (c.value=='-');
        if (einv || c.value=='+') {
            ++c;
        }

        // The reason float constants are used here is that we've seen cases where compilers
        // would perform such casts on compile-time constants at runtime, which would be
        // bad considering how frequently fast_atoreal_move<float> is called in Assimp.
		var cp = Pointer<Pointer<Char>>(arrayOf(c));
        var exp : ai_real = ( strtoul10_64(c, cp) ).a;
		c = cp.value
        if (einv) {
            exp = -exp;
        }
        f *= Math.pow(10.0, exp.toDouble()).a;
    }

    if (inv) {
        f = -f;
    }
    out.value = f;
    return c;
}

fun strtoul10_64(_in : Pointer<Char>, _out : Pointer<Pointer<Char>>?, max_inout : Pointer<Int>? = null) : Long
{
	var in_ = _in; var out_ = _out
    var cur : Int = 0;
    var value : Long = 0.toLong();

    if ( in_.value < '0' || in_.value > '9' )
        throw RuntimeException("The string \"" + in_.value + "\" cannot be converted into a value.");

    var running = true;
    while ( running )
    {
        if ( in_.value < '0' || in_.value > '9' )
            break;

        var new_value : Long = ( value * 10 ) + ( in_.value - '0' );

        // numeric overflow, we rely on you
        if ( new_value < value ) {
            warn("Converting the string \""  + in_.value + "\" into a value resulted in overflow." );
            return 0;
        }
            //throw std::overflow_error();

        value = new_value;

        ++in_;
        ++cur;

        if (max_inout!=null && max_inout!!.value == cur) {

            if (out_!=null) { /* skip to end */
                while (in_.value >= '0' && in_.value <= '9')
                    ++in_;
                out_.value=in_;
            }

            return value;
        }
    }
    if (out_!=null)
        out_.value = in_;

    if (max_inout != null)
        max_inout!!.value = cur;

    return value;
}