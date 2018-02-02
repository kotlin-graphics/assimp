package assimp

import glm_.vec3.Vec3
import java.util.*
import kotlin.collections.ArrayList

class SGSpatialSort {
    private val mPlaneNormal = Vec3(0.8523f, 0.34321f, 0.5736f).normalize()
    private val mPositions = ArrayList<Entry>()

    fun add(vPosition: AiVector3D, index: Int, smoothingGroup: Int)
    {
        val distance = vPosition.distance(mPlaneNormal)
        mPositions.add( Entry( index, vPosition,
                distance, smoothingGroup))
    }

    fun prepare()
    {
        val array = mPositions.toArray() as Array<Entry>
        Arrays.sort(array)
        mPositions.clear()
        mPositions.addAll(array)
    }

    fun findPositions(pPosition: AiVector3D,
    pSG: Int,
    pRadius: Float,
    poResults: ArrayList<Int>,
    exactMatch: Boolean = false)
    {
        val dist = pPosition.distance(mPlaneNormal)
        val minDist = dist - pRadius
        val maxDist = dist + pRadius

        // clear the array
        poResults.clear()

        // quick check for positions outside the range
        if( mPositions.isEmpty() )
            return
        if( maxDist < mPositions[0].distance)
            return
        if( minDist > mPositions[mPositions.size - 1].distance)
            return

        // do a binary search for the minimal distance to start the iteration there
        var index = mPositions.size / 2
        var binaryStepSize = mPositions.size / 4
        while( binaryStepSize > 1)
        {
            if( mPositions[index].distance < minDist)
                index += binaryStepSize
            else
                index -= binaryStepSize

            binaryStepSize /= 2
        }

        // depending on the direction of the last step we need to single step a bit back or forth
        // to find the actual beginning element of the range
        while( index > 0 && mPositions[index].distance > minDist)
            index--
        while( index < (mPositions.size - 1) && mPositions[index].distance < minDist)
            index++

        // Mow start iterating from there until the first position lays outside of the distance range.
        // Add all positions inside the distance range within the given radius to the result aray

        val squareEpsilon = pRadius * pRadius
        val iter = mPositions.iterator()
        for(u in 0 until index){
            iter.next()
        }

        var it = iter.next()
        if (exactMatch)
        {
            while( it.distance < maxDist)
            {
                if((it.position - pPosition).squareLength() < squareEpsilon && it.smoothGroups == pSG) {
                    poResults.add(it.index)
                }
                if(!iter.hasNext()) break
                it = iter.next()
            }
        }
        else
        {
            // if the given smoothing group is 0, we'll return all surrounding vertices
            if (pSG == 0)
            {
                while( it.distance < maxDist)
                {
                    if((it.position - pPosition).squareLength() < squareEpsilon)
                    poResults.add( it.index)
                    if(!iter.hasNext())break
                    it = iter.next()
                }
            }
            else while( it.distance < maxDist)
            {
                if((it.position - pPosition).squareLength() < squareEpsilon &&
                ((it.smoothGroups and pSG) != 0 || it.smoothGroups ==0))
                {
                    poResults.add( it.index)
                }
                if(!iter.hasNext())break
                it = iter.next()
            }
        }
    }

    private class Entry{
        var index = 0    ///< The vertex referred by this entry
        var position = AiVector3D()   ///< Position
        var smoothGroups = 0
        var distance = 0f        ///< Distance of this vertex to the sorting plane

        constructor(pIndex: Int, pPosition: AiVector3D, pDistance: Float, pSG: Int){
            index = pIndex
            position = pPosition
            smoothGroups = pSG
            distance = pDistance
        }

        infix fun lessThan (e: Entry) = distance < e.distance
    }
}