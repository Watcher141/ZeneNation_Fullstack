// src/components/product/StarRating.jsx
import { useState } from 'react';
import { MdStar, MdStarBorder, MdStarHalf } from 'react-icons/md';
import './StarRating.css';

/**
 * Reusable star rating component.
 *
 * Props:
 *  - rating: number (0-5)
 *  - max: number (default 5)
 *  - size: number (default 20)
 *  - interactive: boolean — if true, user can click to set rating
 *  - onChange: (rating) => void — called when user clicks a star
 *  - showNumber: boolean — show numeric rating
 */
const StarRating = ({
  rating = 0,
  max = 5,
  size = 20,
  interactive = false,
  onChange,
  showNumber = false,
  color = '#f5a623',
}) => {
  const [hovered, setHovered] = useState(0);

  const displayRating = interactive ? (hovered || rating) : rating;

  const getStarType = (index) => {
    const filled = displayRating;
    if (filled >= index) return 'full';
    if (filled >= index - 0.5) return 'half';
    return 'empty';
  };

  return (
    <div className={`star-rating ${interactive ? 'interactive' : ''}`}>
      {Array.from({ length: max }, (_, i) => i + 1).map(index => {
        const type = getStarType(index);
        return (
          <span
            key={index}
            className="star"
            onMouseEnter={() => interactive && setHovered(index)}
            onMouseLeave={() => interactive && setHovered(0)}
            onClick={() => interactive && onChange?.(index)}
          >
            {type === 'full'  && <MdStar     size={size} color={color} />}
            {type === 'half'  && <MdStarHalf size={size} color={color} />}
            {type === 'empty' && <MdStarBorder size={size} color={interactive ? (hovered ? color : '#4a4a6a') : '#4a4a6a'} />}
          </span>
        );
      })}
      {showNumber && (
        <span className="star-number">{Number(rating).toFixed(1)}</span>
      )}
    </div>
  );
};

export default StarRating;