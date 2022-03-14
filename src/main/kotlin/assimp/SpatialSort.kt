package assimp

import assimp.format.X.pushBack
import assimp.format.X.resize
import gli_.getSize
import glm_.vec3.Vec3
import kool.BYTES
import java.util.*

class SpatialSort{
    constructor()
    private val mPlaneNormal = Vec3(0.8523f, 0.34321f, 0.5736f).normalize()
    private val mPositions = ArrayList<Entry>()
    private val CHAR_BIT = Character.SIZE

    constructor(pPositions: List<AiVector3D>,pNumPositions: Int,pElementOffset: Int){
        fill(pPositions,pNumPositions,pElementOffset)
    }

    fun fill(pPositions: List<AiVector3D>,pNumPositions: Int,pElementOffset: Int, pFinalize: Boolean = true)
    {
        mPositions.clear()
        append(pPositions,pNumPositions,pElementOffset,pFinalize)
    }

    fun finalize() {
        val array = mPositions.toTypedArray()
        Arrays.sort(array)
        mPositions.clear()
        mPositions.addAll(array)
    }

    fun append(pPositions: List<AiVector3D>,pNumPositions: Int, pElementOffset: Int, pFinalize: Boolean = true)
    {
        // store references to all given positions along with their distance to the reference plane
        val initial = mPositions.size
        for(a in 0 until pNumPositions)
        {
            val vec = pPositions[a]

            // store position by index and distance
            val distance = vec.distance(mPlaneNormal)
            mPositions.add( Entry( a+initial, vec, distance))
        }

        if (pFinalize) {
            // now sort the array ascending by distance.
            finalize()
        }
    }

    fun findPositions(pPosition: AiVector3D,
    pRadius: Float, poResults: ArrayList<Int>)
    {
        val dist = pPosition.distance(mPlaneNormal)
        val minDist = dist - pRadius
        val maxDist = dist + pRadius

        // clear the array
        poResults.clear()

        // quick check for positions outside the range
        if( mPositions.size == 0)
            return
        if( maxDist < mPositions[0].mDistance)
            return
        if( minDist > mPositions[mPositions.size - 1].mDistance)
            return

        // do a binary search for the minimal distance to start the iteration there
        var index = mPositions.size / 2
        var binaryStepSize = mPositions.size / 4
        while( binaryStepSize > 1)
        {
            if( mPositions[index].mDistance < minDist)
                index += binaryStepSize
            else
                index -= binaryStepSize

            binaryStepSize /= 2
        }

        // depending on the direction of the last step we need to single step a bit back or forth
        // to find the actual beginning element of the range
        while( index > 0 && mPositions[index].mDistance > minDist)
            index--
        while( index < (mPositions.size - 1) && mPositions[index].mDistance < minDist)
            index++

        // Mow start iterating from there until the first position lays outside of the distance range.
        // Add all positions inside the distance range within the given radius to the result aray
        val iter = mPositions.iterator()
        while(index > 0){
            iter.next()
            --index
        }
        val pSquared = pRadius*pRadius
        var it = iter.next()
        while( it.mDistance < maxDist)
        {
            if( (it.mPosition - pPosition).squareLength() < pSquared)
            poResults.add(it.mIndex)
            if(!iter.hasNext()) break
            it = iter.next()
        }

        // that's it
    }

    // Fun findIdenticalPositions() - Done
    // TODO: fun toBinary
        // changed parameter type from ai_real to float
    fun toBinary(pValue: Float): Float? {

        assert(UInt.SIZE_BITS >= Float.SIZE_BITS)
        Float.BYTES

        return null




    }

    fun findIdenticalPositions(pPosition: AiVector3D, poResults: ArrayList<Int>) {
        // Epsilons have a huge disadvantage: they are of constant precision, while floating-point
        //  values are of log2 precision. If you apply e=0.01 to 100, the epsilon is rather small, but
        //  if you apply it to 0.001, it is enormous.

        // The best way to overcome this is the unit in the last place (ULP). A precision of 2 ULPs
        //  tells us that a float does not differ more than 2 bits from the "real" value. ULPs are of
        //  logarithmic precision - around 1, they are 1*(2^24) and around 10000, they are 0.00125.

        // For standard C math, we can assume a precision of 0.5 ULPs according to IEEE 754. The
        //  incoming vertex positions might have already been transformed, probably using rather
        //  inaccurate SSE instructions, so we assume a tolerance of 4 ULPs to safely identify
        //  identical vertex positions.
        val toleranceInULPs = 4
        // An interesting point is that the inaccuracy grows linear with the number of operations:
        //  multiplying to numbers, each inaccurate to four ULPs, results in an inaccuracy of four ULPs
        //  plus 0.5 ULPs for the multiplication.
        // To compute the distance to the plane, a dot product is needed - that is a multiplication and
        //  an addition on each number.
        val distanceToleranceInULPs = toleranceInULPs + 1
        // The squared distance between two 3D vectors is computed the same way, but with an additional
        //  subtraction.
        val distance3DToleranceInULPs = distanceToleranceInULPs + 1

        // Convert the plane distance to its signed integer representation so the ULPs tolerance can be
        //  applied. For some reason, VC won't optimize two calls of the bit pattern conversion.
        val minDistBinary = toBinary((pPosition * mPlaneNormal) - distanceToleranceInULPs)
        val maxDistBinary = minDistBinary + 2 * distanceToleranceInULPs
        
        // clear the array in this strange fashion because a simple clear() would also deallocate
        // the array which we want to avoid
        poResults.resize(0) { -> 0 }        // what's our init-lambda?

        // do a binary search for the minimal distance to start the iteration there
        var index = mPositions.size / 2
        var binaryStepSize = mPositions.size / 4
        while ( binaryStepSize > 1){
            // Ugly, but conditional jumps are faster with integers than with floats
            if (minDistBinary > toBinary(mPositions[index].mDistance)) {
                index += binaryStepSize
            }
            else index -= binaryStepSize
            binaryStepSize /= 2
        }

        // depending on the direction of the last step we need to single step a bit back or forth
        // to find the actual beginning element of the range
        while (index > 0 && minDistBinary < toBinary(mPositions[index].mDistance)) index--
        while (index < (mPositions.size-1) && minDistBinary > toBinary(mPositions[index].mDistance)) index ++

        // Now start iterating from there until the first position lays outside of the distance range.
        // Add all positions inside the distance range within the tolerance to the result array
        var it = mPositions.indexOfFirst { it in mPositions } + index
        while (toBinary(mPositions[it].mDistance) < maxDistBinary) {
            if (distance3DToleranceInULPs >= toBinary((mPositions[it].mPosition - pPosition).squareLength()))
                poResults.pushBack(mPositions[it].mIndex)
            it++
            if ( it == mPositions.indexOfLast { it in mPositions })
                break
        }

        // That's it

            /*

                native code:
            std::vector<Entry>::const_iterator it = mPositions.begin() + index;
            while( ToBinary(it->mDistance) < maxDistBinary)
            {
                if( distance3DToleranceInULPs >= ToBinary((it->mPosition - pPosition).SquareLength()))
                    poResults.push_back(it->mIndex);
                    ++it;
                if( it == mPositions.end())
                    break;
    }

        // That's it
             */
    }



    private class Entry {
        var mIndex = 0 ///< The vertex referred by this entry
        var mPosition = AiVector3D() ///< Position
        var mDistance = 0f ///< Distance of this vertex to the sorting plane

        constructor(pIndex: Int, pPosition: AiVector3D, pDistance: Float) {
            mIndex = pIndex
            mPosition = pPosition
            mDistance = pDistance
        }

        fun lessThan(e: Entry): Boolean {
            return mDistance < e.mDistance
        }
    }
}