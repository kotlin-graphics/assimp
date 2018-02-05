package assimp

import glm_.vec3.Vec3
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
        val array = mPositions.toArray() as Array<Entry>
        Arrays.sort(array)
        mPositions.clear()
        mPositions.addAll(array)
    }

    fun append(pPositions: List<AiVector3D>,pNumPositions: Int,pElementOffset: Int, pFinalize: Boolean = true)
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