import { ChatWidget } from './components/ChatWidget';
import './App.css';

function App() {
  return (
    <div className="app">
      <div className="page-content">
        <h1>Agente Flutuante IA</h1>
        <p>Clique no balão flutuante no canto inferior direito para abrir o assistente!</p>
      </div>

      <ChatWidget />
    </div>
  );
}

export default App;
