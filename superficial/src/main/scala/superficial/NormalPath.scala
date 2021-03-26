package superficial

import Polygon.Index

/**
  * A normal arc in a face
  * @param initial the edge containing the initial point
  * @param terminal the edge containing the final point
  * @param face the face containing the arc
  */
case class NormalArc[P<: Polygon](initial: Index, terminal: Index, face: P) {
  val terminalEdge = face.boundary(terminal)

  val initialEdge = face.boundary(initial)

  def vertexLinking = math.abs(terminal - initial) == 1

  def crosses(that: NormalArc[P]) =
    (that.initial - initial) * (that.terminal - terminal) * (that.initial - terminal) * (that.terminal - initial) < 0

}

object NormalArc {
  def enumerate[P <: Polygon](complex: TwoComplex[P]): Set[NormalArc[P]] =
    for {
      face <- complex.faces
      initial <- face.indices
      terminal <- face.indices
      if terminal != initial
    } yield NormalArc(initial, terminal, face)
}

case class PLArc(base: NormalArc[SkewPantsHexagon], initialDisplacement: BigDecimal, finalDisplacement: BigDecimal) {
  // require(!( base.face.boundary(base.initial).isInstanceOf[BoundaryEdge] || base.face.boundary(base.terminal).isInstanceOf[BoundaryEdge] ))
  val hexagonInitialDisplacement: Option[Double] = base.face.boundary(base.initial) match {
    case b: BoundaryEdge => None
    case s: SkewCurveEdge => Some(SkewPantsHexagon.DisplacementFromPBVertex(base.face, s, initialDisplacement.doubleValue))
    case p: PantsSeam => Some(initialDisplacement.doubleValue)
  }
  val hexagonFinalDisplacement: Option[Double] = base.face.boundary(base.terminal) match {
    case b: BoundaryEdge => None
    case s: SkewCurveEdge => Some(SkewPantsHexagon.DisplacementFromPBVertex(base.face, s, finalDisplacement.doubleValue))
    case p: PantsSeam => Some(finalDisplacement.doubleValue)
  }
  val length: Double = {
    require(base.face.edgeLengths.forall(x => x.isDefined) && hexagonInitialDisplacement.isDefined && hexagonFinalDisplacement.isDefined)
    if (base.face.top) {
      Hexagon.Hyperbolic(base.face.edgeLengths(0).get, base.face.edgeLengths(1).get, base.face.edgeLengths(2).get).arcLength(SkewPantsHexagon.SkewIndexToHexagonIndex(base.face, base.initial), SkewPantsHexagon.SkewIndexToHexagonIndex(base.face, base.terminal), hexagonInitialDisplacement.get, hexagonFinalDisplacement.get)
    } else {
      Hexagon.Hyperbolic(base.face.edgeLengths(0).get, base.face.edgeLengths(2).get, base.face.edgeLengths(1).get).arcLength(SkewPantsHexagon.SkewIndexToHexagonIndex(base.face, base.initial), SkewPantsHexagon.SkewIndexToHexagonIndex(base.face, base.terminal), hexagonInitialDisplacement.get, hexagonFinalDisplacement.get)
    }
  }
}

object PLArc {
  def enumerate(arc: NormalArc[SkewPantsHexagon], sep: BigDecimal): Set[PLArc] = {
    require(!(arc.terminalEdge.isInstanceOf[BoundaryEdge] || arc.initialEdge.isInstanceOf[BoundaryEdge]))
    arc.initialEdge match {
      case e1: SkewCurveEdge => arc.terminalEdge match {
        case e2: SkewCurveEdge => for (d1: BigDecimal <- Range.BigDecimal(0, e1.length/2, sep).toSet ; d2: BigDecimal <- Range.BigDecimal(0, e2.length/2, sep).toSet) yield PLArc(arc, d1, d2)
        case s2: PantsSeam => for (d1: BigDecimal <- Range.BigDecimal(0, e1.length/2, sep).toSet ; d2: BigDecimal <- Range.BigDecimal(0, SkewPantsHexagon.getSeamLength(arc.face, s2)/2, sep).toSet) yield PLArc(arc, d1, d2)
      }
      case s1: PantsSeam => arc.terminalEdge match {
        case e2: SkewCurveEdge => for (d1: BigDecimal <- Range.BigDecimal(0, SkewPantsHexagon.getSeamLength(arc.face, s1)/2, sep).toSet ; d2: BigDecimal <- Range.BigDecimal(0, e2.length/2, sep).toSet) yield PLArc(arc, d1, d2)
        case s2: PantsSeam => for (d1: BigDecimal <- Range.BigDecimal(0, SkewPantsHexagon.getSeamLength(arc.face, s1)/2, sep).toSet ; d2: BigDecimal <- Range.BigDecimal(0, SkewPantsHexagon.getSeamLength(arc.face, s2)/2, sep).toSet) yield PLArc(arc, d1, d2)
      }
    }
  }
}

case class NormalPath[P <: Polygon](edges: Vector[NormalArc[P]]) {
  edges.zip(edges.tail).foreach {
    case (e1, e2) =>
      require(
        e1.terminalEdge == e2.initialEdge || e1.terminalEdge == e2.initialEdge.flip,
        s"terminal point on ${e1.terminalEdge} of $e1 is not initial point of $e2 on ${e2.initialEdge}"
      )
      require(!(e1.terminalEdge.isInstanceOf[BoundaryEdge]))
  }

  def +:(arc: NormalArc[P]) = NormalPath(arc +: edges)

  def :+(arc: NormalArc[P]) = NormalPath(edges :+ arc)
  //
  //  def appendOpt(arc: NormalArc): Option[NormalPath] =
  //    if (arc.initial == terminalEdge && arc != edges.last.flip) Some(this :+ arc)
  //    else None

  val isClosed: Boolean = edges.last.terminalEdge == edges.head.initialEdge

  //  val initEdge: Edge = edges.head.initial
  //
  val (terminalFace, terminalIndex) = edges.last.face -> edges.last.terminal

  val terminalEdge =
    terminalFace.boundary(terminalIndex)

  val (initialFace, initialIndex) = edges.head.face -> edges.head.initial

  val initialEdge =
    initialFace.boundary(initialIndex)

  def distinctFaces: Boolean = edges.map(_.face).distinct.size == edges.size

  def linkingPair = edges.zip(edges.tail :+ edges.head).exists {
    case (x, y) => x.vertexLinking && y.vertexLinking
  }

  def distinctEdges = {
    val indEdg = edges.zipWithIndex
    for {
      (x, i) <- indEdg
      (y, j) <- indEdg
    } yield (x, y)
  }

  def withCross = distinctEdges.exists { case (x, y) => x.crosses(y) }

  def geodesicCandidate = !(linkingPair || withCross)
}

object NormalPath {

  @annotation.tailrec
  def enumerateRec[P <: Polygon](
      complex: TwoComplex[P],
      maxAppendLength: Option[Int],
      p: NormalPath[P] => Boolean,
      latest: Set[NormalPath[P]],
      accum: Set[NormalPath[P]]
  ): Set[NormalPath[P]] = {
    if (maxAppendLength.contains(0) || latest.isEmpty) accum
    else {
      val newPaths =
        (
          for {
            path <- latest
            (face, i1) <- complex.edgeIndices(path.terminalEdge).map {
              case (f, i, _) => (f, i)
            } -
              (path.terminalFace -> path.terminalIndex) -
              (path.edges.last.face -> path.edges.last.initial)
            i2 <- face.indices
            if i2 != i1
            arc = NormalArc(i1, i2, face)
          } yield path :+ arc
        ).filter(p)
      enumerateRec(
        complex,
        maxAppendLength.map(_ - 1),
        p,
        newPaths,
        accum union newPaths
      )
    }
  }

  /**
    * recursively enumerate normal paths satisfying a hereditary condition with
    * optional bound on length;
    * terminates if the length is reached or no new paths were generated in the last step.
    * @param complex  the two-complex
    * @param maxLength an optional maximum length
    * @param p a hereditary condition
    * @return set of patbs with bounded length satisfying the condition
    */
  def enumerate[P <: Polygon](
      complex: TwoComplex[P],
      maxLength: Option[Int] = None,
      p: NormalPath[P] => Boolean = (p : NormalPath[P]) => true
  ): Set[NormalPath[P]] =
    if (maxLength.exists(_ < 1)) Set()
    else {
      val lengthOne =
        NormalArc
          .enumerate(complex)
          .map((arc) => NormalPath(Vector(arc)))
          .filter(p)
      enumerateRec(complex, maxLength.map(_ - 1), p, lengthOne, lengthOne)
    }
}

case class PLPath(base: NormalPath[SkewPantsHexagon], initialDisplacements: Vector[BigDecimal], finalDisplacements: Vector[BigDecimal]){
  val PLArcs: Vector[PLArc] = for(i <- (0 to base.edges.size-1).toVector) yield PLArc(base.edges(i), initialDisplacements(i), finalDisplacements(i))
  require(
    PLArcs.zip(PLArcs.tail).forall {
      case (arc1, arc2) => arc1.base.terminalEdge match {
        case s1: SkewCurveEdge => arc2.base.initialEdge match {
          case s2: SkewCurveEdge => SkewCurveEdge.getPos(s1, arc1.finalDisplacement) == SkewCurveEdge.getPos(s2, arc2.initialDisplacement)
          case p2: PantsSeam => false 
        }
        case p1: PantsSeam => arc2.base.initialEdge match {
          case s2: SkewCurveEdge => false
          case p2: PantsSeam => PantsSeam.compareSeamPoints(p1, arc1.finalDisplacement, p2, arc2.initialDisplacement, SkewPantsHexagon.getSeamLength(arc1.base.face, p1))
        }
      }
    }
  )
}
