(set-logic AUFNIRA)
(declare-fun x() Real)
(declare-fun y() Real)
(assert (not (= (* (- 0.0 y) (- (- x) x)) (* (- (- y) y) (- 0.0 x)))))
(check-sat)