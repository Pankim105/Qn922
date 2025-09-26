import React from 'react';
import { RoleplayChat as RoleplayChatComponent } from './roleplay';
import type { User } from './roleplay/types';

interface RoleplayChatProps {
  isAuthenticated: boolean;
  user: User | null;
  onAuthFailure: () => void;
}

const RoleplayChat: React.FC<RoleplayChatProps> = ({ isAuthenticated, user, onAuthFailure }) => {
  return (
    <RoleplayChatComponent
      isAuthenticated={isAuthenticated}
      user={user}
      onAuthFailure={onAuthFailure}
    />
  );
};

export default RoleplayChat;
